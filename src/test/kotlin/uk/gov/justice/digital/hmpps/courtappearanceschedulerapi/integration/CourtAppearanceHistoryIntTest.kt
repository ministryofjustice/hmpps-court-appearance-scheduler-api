package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.set
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceCommentsChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRecategorised
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRelocated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRequestedByVideoLink
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceScheduled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.ManageUsersExtension.Companion.manageUsers
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.ManageUsersServer.Companion.user
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ChangeAppearanceComments
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RecategoriseAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RelocateAppearance
import java.util.UUID

class CourtAppearanceHistoryIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired private val reasonRepository: CourtAppearanceReasonRepository,
) : IntegrationTest(),
  CourtAppearanceOperations by cao {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(URL_TO_TEST, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.SCHEDULER_RO, Roles.SCHEDULER_RW])
  fun `403 forbidden without correct role`(role: String) {
    getAppearanceHistory(newUuid(), role).expectStatus().isForbidden
  }

  @Test
  fun `200 can retrieve court appearance history`() {
    val prison = prisonRegister.givenPrison()
    val ca = givenCourtAppearance(courtAppearance(prisonCode = prison.code))

    val commentsChangedAction = ChangeAppearanceComments(word(25), word(12))
    val commentsUser = manageUsers.givenUser(user(DEFAULT_USERNAME, DEFAULT_NAME))
    transactionTemplate.executeWithoutResult {
      SchedulerContext.get().copy(username = commentsUser.username, reason = commentsChangedAction.reason).set()
      requireNotNull(findCourtAppearance(ca.id)).applyComments(commentsChangedAction)
    }
    val relocateUser = manageUsers.givenUser()
    val relocateAction = RelocateAppearance(courtCode = courtCode(), reason = commentsChangedAction.reason)
    transactionTemplate.executeWithoutResult {
      SchedulerContext.get().copy(username = relocateUser.username, reason = relocateAction.reason).set()
      requireNotNull(findCourtAppearance(ca.id)).relocate(relocateAction)
    }
    val recategoriseUser = manageUsers.givenUser()
    val recategoriseAction = RecategoriseAppearance("VL", word(20))
    transactionTemplate.executeWithoutResult {
      SchedulerContext.get().copy(username = recategoriseUser.username, reason = recategoriseAction.reason).set()
      requireNotNull(findCourtAppearance(ca.id)).recategorise(recategoriseAction, reasonRepository::getReasonByCode)
    }

    SchedulerContext.clear()

    val history = getAppearanceHistory(ca.id).successResponse<AuditHistory>()
    assertThat(history.content).hasSize(4)
    with(history.content.first()) {
      assertThat(user).isEqualTo(AuditedAction.User(SYSTEM_USERNAME, "User $SYSTEM_USERNAME"))
      assertThat(domainEvents).containsExactly(CourtAppearanceScheduled.EVENT_TYPE)
      assertThat(changes).isEmpty()
    }
    with(history.content[1]) {
      assertThat(user).isEqualTo(AuditedAction.User(DEFAULT_USERNAME, DEFAULT_NAME))
      assertThat(domainEvents).containsExactly(CourtAppearanceCommentsChanged.EVENT_TYPE)
      assertThat(reason).isEqualTo(commentsChangedAction.reason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          CourtAppearance::comments.name,
          ca.comments,
          commentsChangedAction.comments,
        ),
      )
    }
    with(history.content[2]) {
      assertThat(user).isEqualTo(AuditedAction.User(relocateUser.username, relocateUser.name))
      assertThat(domainEvents).containsExactly(CourtAppearanceRelocated.EVENT_TYPE)
      assertThat(reason).isEqualTo(relocateAction.reason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          CourtAppearance::courtCode.name,
          ca.courtCode,
          relocateAction.courtCode,
        ),
      )
    }
    with(history.content[3]) {
      assertThat(user).isEqualTo(AuditedAction.User(recategoriseUser.username, recategoriseUser.name))
      assertThat(domainEvents).containsExactlyInAnyOrder(
        CourtAppearanceRecategorised.EVENT_TYPE,
        CourtAppearanceRequestedByVideoLink.EVENT_TYPE,
      )
      assertThat(reason).isEqualTo(recategoriseAction.reason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          CourtAppearance::reason.name,
          ca.reason.description,
          "Video Link (Court Appearance)",
        ),
      )
    }
  }

  private fun getAppearanceHistory(
    id: UUID,
    role: String? = Roles.SCHEDULER_UI,
  ) = webTestClient
    .get()
    .uri(URL_TO_TEST, id)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/court-appearances/{id}/history"
  }
}
