package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement.Direction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations.Companion.unscheduledMovement
import java.util.UUID

class DeleteCourtMovementIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired cmo: CourtMovementOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao,
  CourtMovementOperations by cmo {

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
    deleteMovement(newUuid(), role = role).expectStatus().isForbidden
  }

  @Test
  fun `204 no content when id does not exist`() {
    deleteMovement(newUuid()).expectStatus().isNoContent
  }

  @Test
  fun `204 no content - scheduled movement deleted and appearance status changed`() {
    val appearance = givenCourtAppearance(courtAppearance(movements = listOf(movement(Direction.OUT))))
    val movement = appearance.movements.first()
    assertThat(findCourtMovement(movement.id)).isNotNull

    deleteMovement(movement.id).expectStatus().isNoContent
    assertThat(findCourtMovement(movement.id)).isNull()
    val ca = requireNotNull(findCourtAppearance(appearance.id))
    assertThat(ca.movements).isEmpty()
    assertThat(ca.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)
  }

  @Test
  fun `204 no content - unscheduled movement deleted`() {
    val movement = givenUnscheduledMovement(unscheduledMovement())
    assertThat(findCourtMovement(movement.id)).isNotNull

    deleteMovement(movement.id).expectStatus().isNoContent
    assertThat(findCourtMovement(movement.id)).isNull()
  }

  private fun deleteMovement(
    id: UUID,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .delete()
    .uri(URL_TO_TEST, id)
    .headers(setAuthorisation(username = "NOMIS_SYNC", roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/sync/court-appearance-movements/{id}"
  }
}
