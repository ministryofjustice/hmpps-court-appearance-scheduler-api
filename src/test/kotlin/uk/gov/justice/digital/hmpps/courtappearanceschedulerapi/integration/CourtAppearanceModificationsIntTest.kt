package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement.Direction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceCancelled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceCommentsChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRecategorised
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRelocated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRequestedInPerson
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRescheduled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.externalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.RemandAndSentencingExtension.Companion.rasMockServer
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.CancelAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ChangeAppearanceComments
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.CourtAppearanceAction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.CourtAppearanceActions
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RecategoriseAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RelocateAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RescheduleAppearance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CourtAppearanceModificationsIntTest(
  @Autowired cao: CourtAppearanceOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(URL_TO_TEST, newUuid())
      .bodyValue(CourtAppearanceActions(listOf(ChangeAppearanceComments("401")), "some reason"))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.SCHEDULER_RO, Roles.SCHEDULER_RW])
  fun `403 forbidden without correct role`(role: String) {
    applyAction(newUuid(), ChangeAppearanceComments("403"), role = role).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request if reschedule start and end are null`() {
    val action = RescheduleAppearance(null, null)
    val res = applyAction(newUuid(), action).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: Either start or end must be provided.")
  }

  @Test
  fun `400 bad request if reschedule end is before start`() {
    val action = RescheduleAppearance(
      LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).plusDays(1).plusHours(2),
      LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).plusDays(1),
    )
    val res = applyAction(newUuid(), action).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: End must be after start.")
  }

  @Test
  fun `409 - cannot cancel expired`() {
    val ca = givenCourtAppearance(
      courtAppearance(
        start = LocalDate.now().minusDays(1).atTime(10, 0),
        end = LocalDate.now().minusDays(1).atTime(17, 0),
      ),
    )
    assertThat(ca.status.code).isEqualTo(CourtAppearanceStatus.Code.EXPIRED)

    applyAction(ca.id, CancelAppearance()).errorResponse(HttpStatus.CONFLICT)
  }

  @Test
  fun `409 - cannot cancel in progress`() {
    val ca = givenCourtAppearance(
      courtAppearance(movements = listOf(movement(Direction.OUT))),
    )
    assertThat(ca.status.code).isEqualTo(CourtAppearanceStatus.Code.IN_PROGRESS)

    applyAction(ca.id, CancelAppearance()).errorResponse(HttpStatus.CONFLICT)
  }

  @Test
  fun `409 - cannot cancel completed`() {
    val ca = givenCourtAppearance(
      courtAppearance(
        movements = listOf(movement(Direction.OUT), movement(Direction.IN)),
      ),
    )
    assertThat(ca.status.code).isEqualTo(CourtAppearanceStatus.Code.COMPLETED)

    applyAction(ca.id, CancelAppearance()).errorResponse(HttpStatus.CONFLICT)
  }

  @Test
  fun `409 - cannot cancel ras appearance if ras not available`() {
    val ca = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    assertThat(ca.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)
    rasMockServer.givenDeleteAppearance(ca.externalReference!!.uuid, HttpStatus.SERVICE_UNAVAILABLE)

    applyAction(ca.id, CancelAppearance()).errorResponse(HttpStatus.CONFLICT)
  }

  @Test
  fun `409 - cannot cancel ras appearance if ras returns 409`() {
    val ca = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    assertThat(ca.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)
    rasMockServer.givenDeleteAppearance(ca.externalReference!!.uuid, HttpStatus.CONFLICT)

    applyAction(ca.id, CancelAppearance()).errorResponse(HttpStatus.CONFLICT)
  }

  @Test
  fun `200 - cancel scheduled appearance`() {
    val ca = givenCourtAppearance(courtAppearance())
    assertThat(ca.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)
    val action = CancelAppearance()
    val reason = word(20)
    val username = username()

    val res = applyAction(ca.id, action, reason, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(this.reason).isEqualTo(reason)
      assertThat(domainEvents).containsExactly("person.court-appearance.cancelled")
      assertThat(changes).isEmpty()
    }

    val saved = findCourtAppearance(ca.id)
    assertThat(saved).isNull()

    verifyAudit(
      ca,
      RevisionType.DEL,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = reason),
    )

    verifyEventPublications(
      ca,
      setOf(CourtAppearanceCancelled(ca.person.identifier, ca.id, null).publication(ca.id)),
    )
  }

  @Test
  fun `200 - cancel ras appearance with success response from ras`() {
    val ca = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    assertThat(ca.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)
    rasMockServer.givenDeleteAppearance(ca.externalReference!!.uuid, HttpStatus.NO_CONTENT)

    val action = CancelAppearance()
    val reason = word(20)
    val username = username()

    val res = applyAction(ca.id, action, reason, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(this.reason).isEqualTo(reason)
      assertThat(domainEvents).containsExactly("person.court-appearance.cancelled")
      assertThat(changes).isEmpty()
    }

    val saved = findCourtAppearance(ca.id)
    assertThat(saved).isNull()

    verifyAudit(
      ca,
      RevisionType.DEL,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = reason),
    )

    verifyEventPublications(
      ca,
      setOf(CourtAppearanceCancelled(ca.person.identifier, ca.id, ca.externalReference).publication(ca.id)),
    )
  }

  @Test
  fun `200 can change the comments on a court appearance`() {
    val ca = givenCourtAppearance(courtAppearance())
    val username = username()
    val action = ChangeAppearanceComments(word(20))
    val reason = word(20)
    val res = applyAction(ca.id, action, reason, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(CourtAppearanceCommentsChanged.EVENT_TYPE)
      assertThat(this.reason).isEqualTo(reason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          CourtAppearance::comments.name,
          ca.comments,
          action.comments,
        ),
      )
    }

    val saved = requireNotNull(findCourtAppearance(ca.id))
    assertThat(saved.comments).isEqualTo(action.comments)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = reason),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceCommentsChanged(saved.person.identifier, saved.id, null).publication(saved.id)),
    )
  }

  @Test
  fun `200 can reschedule a court appearance with start and end`() {
    val ca = givenCourtAppearance(courtAppearance())
    val username = username()
    val action = RescheduleAppearance(ca.start.plusDays(1), ca.end!!.plusDays(1))
    val reason = word(20)
    val res = applyAction(ca.id, action, reason, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(CourtAppearanceRescheduled.EVENT_TYPE)
      assertThat(this.reason).isEqualTo(reason)
      assertThat(changes.map { it.propertyName }).containsExactly(
        CourtAppearance::start.name,
        CourtAppearance::end.name,
      )
    }

    val saved = requireNotNull(findCourtAppearance(ca.id))
    assertThat(saved.start.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(action.start!!.truncatedTo(ChronoUnit.SECONDS))
    assertThat(saved.end!!.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(action.end!!.truncatedTo(ChronoUnit.SECONDS))

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = reason),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceRescheduled(saved.person.identifier, saved.id, null).publication(saved.id)),
    )
  }

  @Test
  fun `200 can reschedule a court appearance with start and no end`() {
    val ca = givenCourtAppearance(courtAppearance(end = null))
    val username = username()
    val action = RescheduleAppearance(ca.start.plusDays(1), ca.start.withHour(17).plusDays(1))
    val reason = word(20)
    val res = applyAction(ca.id, action, reason, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(CourtAppearanceRescheduled.EVENT_TYPE)
      assertThat(this.reason).isEqualTo(reason)
      assertThat(changes.map { it.propertyName }).containsExactly(
        CourtAppearance::start.name,
        CourtAppearance::end.name,
      )
    }

    val saved = requireNotNull(findCourtAppearance(ca.id))
    assertThat(saved.start.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(action.start!!.truncatedTo(ChronoUnit.SECONDS))
    assertThat(saved.end!!.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(action.end!!.truncatedTo(ChronoUnit.SECONDS))

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = reason),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceRescheduled(saved.person.identifier, saved.id, null).publication(saved.id)),
    )
  }

  @Test
  fun `200 can recategorise a court appearance`() {
    val ca = givenCourtAppearance(courtAppearance(reasonCode = "VL"))
    assertThat(ca.external).isFalse
    val username = username()
    val action = RecategoriseAppearance("CRT")
    val reason = word(10)
    val res = applyAction(ca.id, action, reason, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactlyInAnyOrder(
        CourtAppearanceRecategorised.EVENT_TYPE,
        CourtAppearanceRequestedInPerson.EVENT_TYPE,
      )
      assertThat(this.reason).isEqualTo(reason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          CourtAppearance::reason.name,
          "Video Link (Court Appearance)",
          "Court Appearance",
        ),
      )
    }

    val saved = requireNotNull(findCourtAppearance(ca.id))
    assertThat(saved.reason.code).isEqualTo(action.reasonCode)
    assertThat(saved.external).isTrue

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = reason),
    )

    verifyEventPublications(
      saved,
      setOf(
        CourtAppearanceRecategorised(saved.person.identifier, saved.id, null).publication(saved.id),
        CourtAppearanceRequestedInPerson(saved.person.identifier, saved.id, null).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 can relocate a court appearance`() {
    val ca = givenCourtAppearance(courtAppearance())
    val username = username()
    val action = RelocateAppearance(courtCode())
    val reason = word(10)
    val res = applyAction(ca.id, action, reason, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(CourtAppearanceRelocated.EVENT_TYPE)
      assertThat(this.reason).isEqualTo(reason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          CourtAppearance::courtCode.name,
          ca.courtCode,
          action.courtCode,
        ),
      )
    }

    val saved = requireNotNull(findCourtAppearance(ca.id))
    assertThat(saved.courtCode).isEqualTo(action.courtCode)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = reason),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceRelocated(saved.person.identifier, saved.id, null).publication(saved.id)),
    )
  }

  private fun applyAction(
    id: UUID,
    action: CourtAppearanceAction,
    reason: String? = word(20),
    username: String = username(),
    role: String? = Roles.SCHEDULER_UI,
  ) = webTestClient
    .put()
    .uri(URL_TO_TEST, id)
    .bodyValue(CourtAppearanceActions(listOf(action), reason))
    .headers(setAuthorisation(username = username, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/court-appearances/{id}"
  }
}
