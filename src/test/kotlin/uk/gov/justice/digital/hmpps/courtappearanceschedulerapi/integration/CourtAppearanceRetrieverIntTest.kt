package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.CourterRegisterExtension.Companion.courtRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Appearance
import java.util.*

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
  fun `200 can retrieve court appearance with prison and court`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()
    val ca = givenCourtAppearance(courtAppearance(prisonCode = prison.code, courtCode = court.code))

    val res = getAppearance(ca.id).successResponse<Appearance>()
    res.verifyAgainst(ca)
    assertThat(res.prison).isEqualTo(prison)
    assertThat(res.court).isEqualTo(court)
  }

  @Test
  fun `200 can retrieve court appearance with default prison and default court`() {
    val prisonCode = prisonCode()
    val courtCode = courtCode()
    prisonRegister.givenPrisons(setOf(), setOf(prisonCode))
    courtRegister.givenCourts(setOf(), setOf(courtCode))
    val ca = givenCourtAppearance(courtAppearance(prisonCode = prisonCode, courtCode = courtCode))

    val res = getAppearance(ca.id).successResponse<Appearance>()
    res.verifyAgainst(ca)
    assertThat(res.prison.name).isEqualTo(prisonCode)
    assertThat(res.court.name).isEqualTo(courtCode)
  }

  private fun Appearance.verifyAgainst(ca: CourtAppearance) {
    assertThat(person.personIdentifier).isEqualTo(ca.person.identifier)
    assertThat(person.firstName).isEqualTo(ca.person.firstName)
    assertThat(person.lastName).isEqualTo(ca.person.lastName)
    assertThat(prison.code).isEqualTo(ca.prisonCode)
    assertThat(court.code).isEqualTo(ca.courtCode)
    assertThat(start).isEqualTo(ca.start)
    assertThat(end).isEqualTo(ca.end)
    assertThat(reason.code).isEqualTo(ca.reason.code)
    assertThat(external).isEqualTo(ca.external)
    assertThat(comments).isEqualTo(ca.comments)
  }

  private fun getAppearance(
    id: UUID,
    role: String? = listOf(Roles.SCHEDULER_RO, Roles.SCHEDULER_RW, Roles.SCHEDULER_UI).random(),
  ) = webTestClient
    .get()
    .uri(URL_TO_TEST, id)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/court-appearances/{id}"
  }
}
