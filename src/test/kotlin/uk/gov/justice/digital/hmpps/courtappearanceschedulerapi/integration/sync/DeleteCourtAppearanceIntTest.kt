package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
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
  fun `409 conflict when appearance has movements`() {
    val appearance = givenCourtAppearance(courtAppearance(movements = listOf(movement(CourtAppearanceMovement.Direction.OUT))))
    deleteAppearance(appearance.id).errorResponse(HttpStatus.CONFLICT)
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
