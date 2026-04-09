package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceCommentsChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRecategorised
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRelocated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRescheduled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ChangeAppearanceComments
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.CourtAppearanceAction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RecategoriseAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RelocateAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RescheduleAppearance
import java.time.temporal.ChronoUnit
import java.util.*

class CourtAppearanceModificationsIntTest(
  @Autowired cao: CourtAppearanceOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(URL_TO_TEST, newUuid())
      .bodyValue(ChangeAppearanceComments("401"))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    applyAction(newUuid(), ChangeAppearanceComments("403"), role = Roles.SCHEDULER_RO)
      .expectStatus().isForbidden
  }

  @Test
  fun `200 can change the comments on a court appearance`() {
    val ca = givenCourtAppearance(courtAppearance())
    val username = username()
    val action = ChangeAppearanceComments(word(20), word(20))
    val res = applyAction(ca.id, action, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(CourtAppearanceCommentsChanged.EVENT_TYPE)
      assertThat(reason).isEqualTo(action.reason)
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
      SchedulerContext.get().copy(username = username, reason = action.reason),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceCommentsChanged(saved.person.identifier, saved.id).publication(saved.id)),
    )
  }

  @Test
  fun `200 can reschedule a court appearance with start and end`() {
    val ca = givenCourtAppearance(courtAppearance())
    val username = username()
    val action = RescheduleAppearance(ca.start.plusDays(1), ca.end!!.plusDays(1), word(10))
    val res = applyAction(ca.id, action, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(CourtAppearanceRescheduled.EVENT_TYPE)
      assertThat(reason).isEqualTo(action.reason)
      assertThat(changes.map { it.propertyName }).containsExactly(CourtAppearance::start.name, CourtAppearance::end.name)
    }

    val saved = requireNotNull(findCourtAppearance(ca.id))
    assertThat(saved.start.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(action.start!!.truncatedTo(ChronoUnit.SECONDS))
    assertThat(saved.end!!.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(action.end!!.truncatedTo(ChronoUnit.SECONDS))

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = action.reason),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceRescheduled(saved.person.identifier, saved.id).publication(saved.id)),
    )
  }

  @Test
  fun `200 can reschedule a court appearance with start and no end`() {
    val ca = givenCourtAppearance(courtAppearance(end = null))
    val username = username()
    val action = RescheduleAppearance(ca.start.plusDays(1), ca.start.withHour(17).plusDays(1), word(10))
    val res = applyAction(ca.id, action, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(CourtAppearanceRescheduled.EVENT_TYPE)
      assertThat(reason).isEqualTo(action.reason)
      assertThat(changes.map { it.propertyName }).containsExactly(CourtAppearance::start.name, CourtAppearance::end.name)
    }

    val saved = requireNotNull(findCourtAppearance(ca.id))
    assertThat(saved.start.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(action.start!!.truncatedTo(ChronoUnit.SECONDS))
    assertThat(saved.end!!.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(action.end!!.truncatedTo(ChronoUnit.SECONDS))

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = action.reason),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceRescheduled(saved.person.identifier, saved.id).publication(saved.id)),
    )
  }

  @Test
  fun `200 can recategorise a court appearance`() {
    val ca = givenCourtAppearance(courtAppearance(reasonCode = "VL"))
    assertThat(ca.external).isFalse
    val username = username()
    val action = RecategoriseAppearance("CRT", word(10))
    val res = applyAction(ca.id, action, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(CourtAppearanceRecategorised.EVENT_TYPE)
      assertThat(reason).isEqualTo(action.reason)
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
      SchedulerContext.get().copy(username = username, reason = action.reason),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceRecategorised(saved.person.identifier, saved.id).publication(saved.id)),
    )
  }

  @Test
  fun `200 can relocate a court appearance`() {
    val ca = givenCourtAppearance(courtAppearance())
    val username = username()
    val action = RelocateAppearance(courtCode(), word(10))
    val res = applyAction(ca.id, action, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(CourtAppearanceRelocated.EVENT_TYPE)
      assertThat(reason).isEqualTo(action.reason)
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
      SchedulerContext.get().copy(username = username, reason = action.reason),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceRelocated(saved.person.identifier, saved.id).publication(saved.id)),
    )
  }

  private fun applyAction(
    id: UUID,
    action: CourtAppearanceAction,
    username: String = username(),
    role: String? = listOf(Roles.SCHEDULER_RW, Roles.SCHEDULER_UI).random(),
  ) = webTestClient
    .put()
    .uri(URL_TO_TEST, id)
    .bodyValue(action)
    .headers(setAuthorisation(username = username, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/court-appearances/{id}"
  }
}
