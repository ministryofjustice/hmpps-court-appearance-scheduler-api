package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEvent
import java.util.UUID

class CourtAppearanceRetrieverIntTest(
  @Autowired cao: CourtAppearanceOperations,
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

  @Test
  fun `403 forbidden without correct role`() {
    getAppearance(newUuid(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `404 not found when no appearance with provided id`() {
    getAppearance(newUuid()).errorResponse(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `200 can retrieve court appearance`() {
    val ca = givenCourtAppearance(CourtAppearanceOperations.courtAppearance())
    val res = getAppearance(ca.id).successResponse<CourtEvent>()
    ca verifyAgainst res
  }

  private fun getAppearance(
    id: UUID,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .get()
    .uri(URL_TO_TEST, id)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/sync/court-appearances/{id}"
  }
}
