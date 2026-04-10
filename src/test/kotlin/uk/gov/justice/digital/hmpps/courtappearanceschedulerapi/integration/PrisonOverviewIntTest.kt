package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.PrisonOverview
import java.time.LocalDate

class PrisonOverviewIntTest(
  @Autowired cao: CourtAppearanceOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(URL_TO_TEST, "POU")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.SCHEDULER_RW, Roles.SCHEDULER_RO])
  fun `403 forbidden without correct role`(role: String) {
    getOverview("FBD", role).expectStatus().isForbidden
  }

  @Test
  fun `counts leaving and returning today returned when no results`() {
    val prisonCode = prisonCode()
    val response = getOverview(prisonCode).successResponse<PrisonOverview>()
    assertThat(response).isEqualTo(PrisonOverview(0, 0))
  }

  @Test
  fun `counts leaving and returning today are correctly returned`() {
    val prisonCode = prisonCode()
    (0..4).forEach {
      givenCourtAppearance(
        courtAppearance(
          prisonCode = prisonCode,
          reasonCode = if (it % 2 == 0) "CRT" else "VL",
          start = LocalDate.now().atTime(10, 0),
          end = if (it % 4 == 0) null else LocalDate.now().atTime(17, 0),
        ),
      )
    }

    val response = getOverview(prisonCode).successResponse<PrisonOverview>()
    assertThat(response).isEqualTo(PrisonOverview(3, 3))
  }

  private fun getOverview(
    prisonCode: String,
    role: String? = Roles.SCHEDULER_UI,
  ) = webTestClient
    .get()
    .uri(URL_TO_TEST, prisonCode)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/prisons/{prisonCode}/external-movements/overview"
  }
}
