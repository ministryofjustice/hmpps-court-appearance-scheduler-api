package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.AppearanceMovementCommentsChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.AppearanceMovementRecategorised
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.AppearanceMovementRecorded
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.AppearanceMovementRelocated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceCompleted
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations.Companion.unscheduledMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync.SyncGenerator.courtEventMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync.SyncGenerator.syncUser
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEventMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.SyncCourtEventMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.User
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SyncCourtMovementIntTest(
  @Autowired cmo: CourtMovementOperations,
  @Autowired cao: CourtAppearanceOperations,
  @Autowired pso: PersonSummaryOperations,
) : IntegrationTest(),
  CourtMovementOperations by cmo,
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
    syncMovement(personIdentifier(), syncRequest(), role = role)
      .expectStatus().isForbidden
  }

  @Test
  fun `404 not found - occurrence does not exist`() {
    val person = givenPersonSummary(personSummary())
    val request = syncRequest(courtEventMovement(newUuid()))
    syncMovement(person.identifier, request).errorResponse(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `200 ok - new unscheduled movement created for new person`() {
    val prisonCode = prisonCode()
    val prisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode))

    val request = syncRequest(courtEventMovement(fromAgencyId = prisonCode), syncUser(activeCaseloadId = prisonCode))
    val response = syncMovement(prisoner.prisonerNumber, request).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtMovement(response.id))
    saved verifyAgainst request.movement

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearanceMovement::class.simpleName!!),
      SchedulerContext.get().copy(
        username = request.user.username,
        caseloadId = request.user.activeCaseloadId,
        source = DataSource.NOMIS,
      ),
    )

    verifyEventPublications(
      saved,
      setOf(AppearanceMovementRecorded(saved.person.identifier, saved.id, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 ok - new unscheduled movement created for existing person`() {
    val person = givenPersonSummary(personSummary())
    val prisonCode = person.prisonCode!!

    val request = syncRequest(courtEventMovement(fromAgencyId = prisonCode), syncUser(activeCaseloadId = prisonCode))
    val response = syncMovement(person.identifier, request).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtMovement(response.id))
    saved verifyAgainst request.movement

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearanceMovement::class.simpleName!!),
      SchedulerContext.get().copy(
        username = request.user.username,
        caseloadId = request.user.activeCaseloadId,
        source = DataSource.NOMIS,
      ),
    )

    verifyEventPublications(
      saved,
      setOf(AppearanceMovementRecorded(saved.person.identifier, saved.id, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 ok - new scheduled movement created and schedule status updated`() {
    val appearance =
      givenCourtAppearance(courtAppearance(movements = listOf(movement(CourtAppearanceMovement.Direction.OUT))))
    val prisonCode = appearance.prisonCode

    val request =
      syncRequest(courtEventMovement(scheduleId = appearance.id, fromAgencyId = prisonCode, directionCode = "IN"))
    val response = syncMovement(appearance.person.identifier, request).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtMovement(response.id))
    saved verifyAgainst request.movement
    assertThat(saved.courtAppearance?.status?.code).isEqualTo(CourtAppearanceStatus.Code.COMPLETED)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        CourtAppearanceMovement::class.simpleName!!,
        CourtAppearance::class.simpleName!!,
      ),
      SchedulerContext.get().copy(
        username = request.user.username,
        caseloadId = request.user.activeCaseloadId,
        source = DataSource.NOMIS,
      ),
    )

    val updatedAppearance = requireNotNull(saved.courtAppearance)
    verifyEventPublications(
      saved,
      setOf(
        AppearanceMovementRecorded(saved.person.identifier, saved.id, DataSource.NOMIS).publication(saved.id),
        CourtAppearanceCompleted(
          updatedAppearance.person.identifier,
          updatedAppearance.id,
          DataSource.NOMIS,
        ).publication(updatedAppearance.id),
      ),
    )
  }

  @Test
  fun `200 ok - movement id returned if legacy id already exists`() {
    val bookingId = newId()
    val movementSeq = 1
    val appearance = givenCourtAppearance(
      courtAppearance(
        movements = listOf(
          movement(
            CourtAppearanceMovement.Direction.OUT,
            legacyId = "${bookingId}_$movementSeq",
          ),
        ),
      ),
    )
    assertThat(appearance.status.code).isEqualTo(CourtAppearanceStatus.Code.IN_PROGRESS)
    val movement = appearance.movements.first()

    val request = syncRequest(
      courtEventMovement(
        scheduleId = appearance.id,
        bookingId = bookingId,
        sequenceNumber = movementSeq,
        fromAgencyId = movement.prisonCode,
        toAgencyId = movement.courtCode,
        commentText = movement.comments,
        date = movement.occurredAt.toLocalDate(),
        time = movement.occurredAt.toLocalTime(),
      ),
    )
    val response = syncMovement(appearance.person.identifier, request).successResponse<ReferenceId>()

    assertThat(response.id).isEqualTo(movement.id)

    verifyAudit(
      movement,
      RevisionType.ADD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        CourtAppearanceMovement::class.simpleName!!,
        CourtAppearance::class.simpleName!!,
      ),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME),
    )
  }

  @Test
  fun `200 ok - movement updated`() {
    val bookingId = newId()
    val movementSeq = 1
    val movement = givenUnscheduledMovement(unscheduledMovement(legacyId = "${bookingId}_$movementSeq"))

    val request = syncRequest(
      courtEventMovement(
        bookingId = bookingId,
        sequenceNumber = movementSeq,
        fromAgencyId = movement.prisonCode,
        date = movement.occurredAt.toLocalDate(),
        time = movement.occurredAt.toLocalTime(),
        reasonCode = "VL",
      ),
    )

    val res = syncMovement(movement.person.identifier, request).successResponse<ReferenceId>()

    assertThat(res.id).isEqualTo(movement.id)
    val saved = requireNotNull(findCourtMovement(res.id))
    saved verifyAgainst request.movement

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        CourtAppearanceMovement::class.simpleName!!,
      ),
      SchedulerContext.get().copy(
        username = request.user.username,
        caseloadId = request.user.activeCaseloadId,
        source = DataSource.NOMIS,
      ),
    )

    verifyEventPublications(
      saved,
      setOf(
        AppearanceMovementCommentsChanged(saved.person.identifier, saved.id, DataSource.NOMIS).publication(saved.id),
        AppearanceMovementRecategorised(saved.person.identifier, saved.id, DataSource.NOMIS).publication(saved.id),
        AppearanceMovementRelocated(saved.person.identifier, saved.id, DataSource.NOMIS).publication(saved.id),
      ),
    )
  }

  private fun syncRequest(
    movement: CourtEventMovement = courtEventMovement(),
    user: User = syncUser(),
    occurredAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
  ) = SyncCourtEventMovement(occurredAt, user, movement)

  private fun syncMovement(
    personIdentifier: String,
    request: SyncCourtEventMovement,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(URL_TO_TEST, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = "NOMIS_SYNC", roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/sync/court-appearance-movements/{personIdentifier}"
  }
}
