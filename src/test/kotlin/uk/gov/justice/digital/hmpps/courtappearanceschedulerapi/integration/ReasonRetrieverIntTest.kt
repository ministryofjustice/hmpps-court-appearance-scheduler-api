package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AppearanceReason
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.CourtAppearanceReasons

class ReasonRetrieverIntTest : IntegrationTest() {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(URL_TO_TEST)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getReasons("ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `items are sorted by sequence number`() {
    val reasons = getReasons().expectStatus().isOk.expectBody<CourtAppearanceReasons>().returnResult().responseBody!!

    assertThat(reasons.items).containsExactly(
      AppearanceReason(code = "CE", description = "Cond Release Extended Sentence CJA 2003", external = true),
      AppearanceReason(code = "CRT", description = "Court Appearance", external = true),
      AppearanceReason(code = "19", description = "Court Appearance - Police Product Order", external = true),
      AppearanceReason(code = "22", description = "Witness", external = true),
      AppearanceReason(code = "AP", description = "Discharge to Court on Appeal", external = true),
      AppearanceReason(code = "DC", description = "Discharged to Court", external = true),
      AppearanceReason(code = "PR", description = "Production of Unsentenced Inmate at Court", external = true),
      AppearanceReason(code = "PS", description = "Production (Sentence/Civil Custody)", external = true),
      AppearanceReason(code = "VLAP", description = "Video Link Appeal", external = false),
      AppearanceReason(code = "VLBL", description = "Video Link Bail Application", external = false),
      AppearanceReason(code = "VLBH", description = "Video Link Breach Hearing", external = false),
      AppearanceReason(code = "VLCV", description = "Video Link Civil Hearing", external = false),
      AppearanceReason(code = "VLCH", description = "Video Link Committal Hearing", external = false),
      AppearanceReason(code = "VLCR", description = "Video Link Court Review", external = false),
      AppearanceReason(code = "VLEB", description = "Video Link Expedited Breach Hearing", external = false),
      AppearanceReason(code = "VLNH", description = "Video Link Newton Hearing", external = false),
      AppearanceReason(code = "VLPD", description = "Video Link Plea and Direction Hearing", external = false),
      AppearanceReason(code = "VLPP", description = "Video Link Police Production", external = false),
      AppearanceReason(code = "VLPC", description = "Video Link Proceeds of Crime Hearing", external = false),
      AppearanceReason(code = "VLPS", description = "Video Link Production (Sentencing)", external = false),
      AppearanceReason(code = "VLPR", description = "Video Link Production (Unsent.)", external = false),
      AppearanceReason(code = "VLRH", description = "Video Link Remittal Hearing", external = false),
      AppearanceReason(code = "VLWT", description = "Video Link Witness Production", external = false),
      AppearanceReason(code = "VL", description = "Video Link (Court Appearance)", external = false),
      AppearanceReason(code = "EXP_BREACH", description = "Expedited Breach Hearing", external = true),
      AppearanceReason(code = "BAIL", description = "Bail application", external = true),
      AppearanceReason(code = "BREACH", description = "Breach hearing", external = true),
      AppearanceReason(code = "COM", description = "Committal Hearing", external = true),
      AppearanceReason(code = "CRI", description = "Court Review", external = true),
      AppearanceReason(code = "NEWTON", description = "Newton Hearing", external = true),
      AppearanceReason(code = "PDH", description = "Plea and Directions Hearing", external = true),
      AppearanceReason(code = "REM", description = "Remittal Hearing", external = true),
      AppearanceReason(code = "SENT", description = "Sentencing", external = true),
      AppearanceReason(code = "TRIAL", description = "Trial", external = true),
    )
  }

  private fun getReasons(
    role: String? = listOf(Roles.SCHEDULER_RO, Roles.SCHEDULER_RW, Roles.SCHEDULER_UI).random(),
  ) = webTestClient
    .get()
    .uri(URL_TO_TEST)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/court-appearance-reasons"
  }
}
