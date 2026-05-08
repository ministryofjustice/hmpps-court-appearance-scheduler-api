package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations.Companion.unscheduledMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEventMovement
import java.util.UUID

class CourtMovementRetrieverIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired cam: CourtMovementOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao,
  CourtMovementOperations by cam {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(URL_TO_TEST, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getMovement(newUuid(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `404 not found when no appearance with provided id`() {
    getMovement(newUuid()).errorResponse(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `200 can retrieve scheduled movement`() {
    val ca = givenCourtAppearance(courtAppearance(movements = listOf(movement(CourtAppearanceMovement.Direction.OUT))))
    val cm = ca.movements.single()
    val res = getMovement(cm.id).successResponse<CourtEventMovement>()
    cm verifyAgainst res
  }

  @Test
  fun `200 can retrieve unscheduled movement`() {
    val cm = givenUnscheduledMovement(unscheduledMovement(direction = CourtAppearanceMovement.Direction.IN))
    val res = getMovement(cm.id).successResponse<CourtEventMovement>()
    cm verifyAgainst res
  }

  private fun getMovement(
    id: UUID,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .get()
    .uri(URL_TO_TEST, id)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/sync/court-appearance-movements/{id}"
  }
}
