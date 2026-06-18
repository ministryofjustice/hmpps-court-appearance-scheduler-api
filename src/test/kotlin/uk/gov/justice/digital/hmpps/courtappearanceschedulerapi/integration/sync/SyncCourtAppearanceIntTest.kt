package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceCommentsChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceCompleted
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRecorded
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRelocated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRescheduled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceScheduled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceUnscheduled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.externalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync.SyncGenerator.courtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync.SyncGenerator.syncUser
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.SyncCourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.User
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SyncCourtAppearanceIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired pso: PersonSummaryOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao,
  PersonSummaryOperations by pso {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(URL_TO_TEST, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.SCHEDULER_RO, Roles.SCHEDULER_RW, Roles.SCHEDULER_UI])
  fun `403 forbidden without correct role`(role: String) {
    syncAppearance(personIdentifier(), syncRequest(), role = role)
      .expectStatus().isForbidden
  }

  @ParameterizedTest
  @MethodSource("invalidExternalReferenceValues")
  fun `400 bad request if external reference is not valid`(urn: String) {
    val request: ObjectNode = jsonMapper.valueToTree(syncRequest())
    val courtEvent: ObjectNode = (request.get("courtEvent") as ObjectNode)
    courtEvent.put("externalReferenceUrn", urn)

    val response = syncAppearanceForExternalReferenceValidation(personIdentifier(), request)
      .errorResponse(HttpStatus.BAD_REQUEST)

    assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(response.userMessage).isEqualTo("Invalid request")
    assertThat(response.developerMessage).startsWith("HttpMessageNotReadableException:")
  }

  @Test
  fun `200 ok - court appearance scheduled for new person`() {
    val prisonCode = prisonCode()
    val prisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode))

    val request = syncRequest(courtEvent(prisonCode))
    val response = syncAppearance(prisoner.prisonerNumber, request).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtAppearance(response.id))
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)
    saved verifyAgainst request.courtEvent

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearance::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = request.user.username, caseloadId = request.user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceScheduled(saved.person.identifier, saved.id, null, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 ok - court appearance scheduled for existing person`() {
    val person = givenPersonSummary(personSummary())
    val prisonCode = requireNotNull(person.prisonCode)

    val request = syncRequest(courtEvent(prisonCode))
    val response = syncAppearance(person.identifier, request).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtAppearance(response.id))
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)
    saved verifyAgainst request.courtEvent

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearance::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = request.user.username, caseloadId = request.user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceScheduled(saved.person.identifier, saved.id, null, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 ok - next court appearance scheduled for RaS`() {
    val person = givenPersonSummary(personSummary())
    val prisonCode = requireNotNull(person.prisonCode)

    val request = syncRequest(courtEvent(prisonCode, externalReference = externalReference()))
    val response = syncAppearance(person.identifier, request).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtAppearance(response.id))
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)
    saved verifyAgainst request.courtEvent

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearance::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = request.user.username, caseloadId = request.user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        CourtAppearanceScheduled(
          saved.person.identifier,
          saved.id,
          saved.externalReference,
          DataSource.NOMIS,
        ).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 ok scheduled appearance id returned if legacy id already exists`() {
    val appearance = givenCourtAppearance(courtAppearance(legacyId = newId()))
    val request = with(appearance) {
      syncRequest(
        courtEvent(
          prisonCode,
          courtCode,
          reason.code,
          date = appearance.start.toLocalDate(),
          startTime = appearance.start.toLocalTime(),
          commentText = appearance.comments,
          eventId = appearance.legacyId!!,
        ),
      )
    }

    val response = syncAppearance(appearance.person.identifier, request).successResponse<ReferenceId>()

    assertThat(response.id).isEqualTo(appearance.id)
    verifyAudit(
      appearance,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearance::class.simpleName!!),
    )
  }

  @Test
  fun `200 ok scheduled appearance id and legacy id returned if external reference already exists`() {
    val appearance = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    val request = with(appearance) {
      syncRequest(
        courtEvent(
          prisonCode,
          courtCode,
          reason.code,
          date = appearance.start.toLocalDate(),
          startTime = appearance.start.toLocalTime(),
          commentText = appearance.comments,
          externalReference = appearance.externalReference!!,
        ),
      )
    }

    val response = syncAppearance(appearance.person.identifier, request).successResponse<ReferenceId>()

    assertThat(response.id).isEqualTo(appearance.id)
    verifyAudit(
      appearance,
      RevisionType.MOD,
      setOf(CourtAppearance::class.simpleName!!),
      SchedulerContext.get().copy(username = request.user.username, caseloadId = request.user.activeCaseloadId, source = DataSource.NOMIS),
    )
  }

  @Test
  fun `200 ok - court appearance updated`() {
    val appearance = givenCourtAppearance(courtAppearance(legacyId = newId()))

    val request =
      syncRequest(courtEvent(appearance.prisonCode, eventId = appearance.legacyId!!, externalReference = externalReference()))
    val response = syncAppearance(appearance.person.identifier, request).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtAppearance(response.id))
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)
    saved verifyAgainst request.courtEvent

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearance::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = request.user.username, caseloadId = request.user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        CourtAppearanceRelocated(
          saved.person.identifier,
          saved.id,
          request.courtEvent.externalReferenceUrn,
          DataSource.NOMIS,
        ).publication(saved.id),
        CourtAppearanceRescheduled(
          saved.person.identifier,
          saved.id,
          request.courtEvent.externalReferenceUrn,
          DataSource.NOMIS,
        ).publication(saved.id),
        CourtAppearanceCommentsChanged(
          saved.person.identifier,
          saved.id,
          request.courtEvent.externalReferenceUrn,
          DataSource.NOMIS,
        ).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 ok - court appearance sent as complete`() {
    val person = givenPersonSummary(personSummary())
    val prisonCode = requireNotNull(person.prisonCode)

    val request = syncRequest(courtEvent(prisonCode, status = "COMP", externalReference = externalReference()))
    val response = syncAppearance(person.identifier, request).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtAppearance(response.id))
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.COMPLETED)
    saved verifyAgainst request.courtEvent

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearance::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = request.user.username, caseloadId = request.user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceRecorded(saved.person.identifier, saved.id, saved.externalReference, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 ok - court appearance completed by sync`() {
    val appearance = givenCourtAppearance(courtAppearance(legacyId = newId()))

    val request = syncRequest(
      courtEvent(
        scheduledPrisonCode = appearance.prisonCode,
        scheduledCourtCode = appearance.courtCode,
        eventId = appearance.legacyId!!,
        status = "COMP",
        externalReference = externalReference(),
        date = appearance.start.toLocalDate(),
        startTime = appearance.start.toLocalTime(),
        commentText = appearance.comments,
      ),
    )
    val response = syncAppearance(appearance.person.identifier, request).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtAppearance(response.id))
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.COMPLETED)
    saved verifyAgainst request.courtEvent

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearance::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = request.user.username, caseloadId = request.user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        CourtAppearanceCompleted(
          saved.person.identifier,
          saved.id,
          request.courtEvent.externalReferenceUrn,
          DataSource.NOMIS,
        ).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 ok - court appearance unscheduled by sync`() {
    val appearance = givenCourtAppearance(courtAppearance(legacyId = newId()))
    assertThat(appearance.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)

    val request = syncRequest(
      courtEvent(
        scheduledPrisonCode = appearance.prisonCode,
        scheduledCourtCode = appearance.courtCode,
        eventId = appearance.legacyId!!,
        status = "SCHED",
        externalReference = externalReference(),
        date = appearance.start.toLocalDate(),
        startTime = appearance.start.toLocalTime(),
        commentText = appearance.comments,
        currentTerm = false,
      ),
    )
    val response = syncAppearance(appearance.person.identifier, request).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtAppearance(response.id))
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.UNSCHEDULED)
    saved verifyAgainst request.courtEvent

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearance::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = request.user.username, caseloadId = request.user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        CourtAppearanceUnscheduled(
          saved.person.identifier,
          saved.id,
          request.courtEvent.externalReferenceUrn,
          DataSource.NOMIS,
        ).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 ok - unscheduled court appearance scheduled by sync`() {
    val appearance = givenCourtAppearance(courtAppearance(legacyId = newId(), unschedule = true))
    assertThat(appearance.status.code).isEqualTo(CourtAppearanceStatus.Code.UNSCHEDULED)

    val request = syncRequest(
      courtEvent(
        scheduledPrisonCode = appearance.prisonCode,
        scheduledCourtCode = appearance.courtCode,
        eventId = appearance.legacyId!!,
        status = "SCHED",
        externalReference = externalReference(),
        date = appearance.start.toLocalDate(),
        startTime = appearance.start.toLocalTime(),
        commentText = appearance.comments,
        currentTerm = true,
      ),
    )
    val response = syncAppearance(appearance.person.identifier, request).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtAppearance(response.id))
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)
    saved verifyAgainst request.courtEvent

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearance::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = request.user.username, caseloadId = request.user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        CourtAppearanceScheduled(
          saved.person.identifier,
          saved.id,
          request.courtEvent.externalReferenceUrn,
          DataSource.NOMIS,
        ).publication(saved.id),
      ),
    )
  }

  private fun syncRequest(
    courtEvent: CourtEvent = courtEvent(),
    user: User = syncUser(activeCaseloadId = courtEvent.scheduledPrisonCode),
    occurredAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  ) = SyncCourtEvent(occurredAt, user, courtEvent)

  private fun syncAppearance(
    personIdentifier: String,
    request: SyncCourtEvent,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(URL_TO_TEST, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = "NOMIS_SYNC", roles = listOfNotNull(role)))
    .exchange()

  private fun syncAppearanceForExternalReferenceValidation(
    personIdentifier: String,
    request: JsonNode,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(URL_TO_TEST, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = "NOMIS_SYNC", roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/sync/court-appearances/{personIdentifier}"

    @JvmStatic
    fun invalidExternalReferenceValues() = listOf(
      "urn:some-other-service:some-other-entity:${newUuid()}",
      "urn:some-other-service:court-appearance:${newUuid()}",
      "urn:remand-and-sentencing:some-other-entity:${newUuid()}",
      "urn:remand-and-sentencing:court-appearance:invalid-uuid",
      "remand-and-sentencing:court-appearance:${newUuid()}",
    )
  }
}
