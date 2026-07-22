package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceCancelled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import java.util.UUID

class DeleteCourtAppearanceIntTest(
  @Autowired cao: CourtAppearanceOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .delete()
      .uri(URL_TO_TEST, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.SCHEDULER_RO, Roles.SCHEDULER_RW, Roles.SCHEDULER_UI])
  fun `403 forbidden without correct role`(role: String) {
    deleteAppearance(newUuid(), role = role).expectStatus().isForbidden
  }

  @Test
  fun `204 - appearance with movements deleted - movements orphaned`() {
    val appearance = givenCourtAppearance(
      courtAppearance(movements = listOf(movement(CourtAppearanceMovement.Direction.OUT))),
    )
    deleteAppearance(appearance.id).expectStatus().isNoContent

    assertThat(findCourtAppearance(appearance.id)).isNull()

    verifyAudit(
      appearance,
      RevisionType.DEL,
      setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearance::class.simpleName!!, CourtAppearanceMovement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = SYSTEM_USERNAME, caseloadId = null, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      appearance,
      setOf(
        CourtAppearanceCancelled(
          appearance.person.identifier,
          appearance.id,
          appearance.externalReference,
          DataSource.NOMIS,
        ).publication(appearance.id),
      ),
    )
  }

  @Test
  fun `204 no content when id does not exist`() {
    deleteAppearance(newUuid()).expectStatus().isNoContent
  }

  @Test
  fun `204 no content - appearance deleted`() {
    val appearance = givenCourtAppearance(courtAppearance())
    deleteAppearance(appearance.id).expectStatus().isNoContent

    assertThat(findCourtAppearance(appearance.id)).isNull()
  }

  private fun deleteAppearance(
    id: UUID,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .delete()
    .uri(URL_TO_TEST, id)
    .headers(setAuthorisation(username = "NOMIS_SYNC", roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/sync/court-appearances/{id}"
  }
}
