package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.externalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.AppearanceDeletionStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.CourterRegisterExtension.Companion.courtRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.RemandAndSentencingExtension.Companion.rasMockServer
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Appearance
import java.time.LocalDate
import java.time.LocalDateTime
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
  fun `404 not found if not record exists`() {
    getAppearance(newUuid()).expectStatus().isNotFound
  }

  @Test
  fun `400 bad request if id is not a valid uuid`() {
    val res = getAppearance("invalid-uuid").errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.userMessage).isEqualTo("Method argument type mismatch, expecting class java.util.UUID")
  }

  @Test
  fun `200 can retrieve cancellable court appearance with prison and court`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()
    val ca = givenCourtAppearance(courtAppearance(prisonCode = prison.code, courtCode = court.code))
    assertThat(ca.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)

    val res = getAppearance(ca.id).successResponse<Appearance>()
    res.verifyAgainst(ca)
    assertThat(res.prison).isEqualTo(prison)
    assertThat(res.court).isEqualTo(court)
    assertThat(res.cancellable).isTrue
  }

  @Test
  fun `200 can retrieve non-cancellable court appearance with prison and court`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()
    val ca = givenCourtAppearance(
      courtAppearance(
        prisonCode = prison.code,
        courtCode = court.code,
        start = LocalDate.now().minusDays(1).atTime(10, 0),
        end = LocalDate.now().minusDays(1).atTime(17, 0),
      ),
    )
    assertThat(ca.status.code).isEqualTo(CourtAppearanceStatus.Code.EXPIRED)

    val res = getAppearance(ca.id).successResponse<Appearance>()
    res.verifyAgainst(ca)
    assertThat(res.prison).isEqualTo(prison)
    assertThat(res.court).isEqualTo(court)
    assertThat(res.cancellable).isFalse
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

  @Test
  fun `200 can retrieve court appearance with external reference and delete supported`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()
    val ca = givenCourtAppearance(
      courtAppearance(
        prisonCode = prison.code,
        courtCode = court.code,
        externalReference = externalReference(),
      ),
    )
    rasMockServer.givenDeletionStatus(ca.externalReference!!.uuid, AppearanceDeletionStatus.SUPPORTED)

    val res = getAppearance(ca.id).successResponse<Appearance>()
    res.verifyAgainst(ca)
    assertThat(res.prison).isEqualTo(prison)
    assertThat(res.court).isEqualTo(court)
    assertThat(res.cancellable).isTrue
  }

  @Test
  fun `200 can retrieve court appearance with external reference and delete not supported`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()
    val ca = givenCourtAppearance(
      courtAppearance(
        prisonCode = prison.code,
        courtCode = court.code,
        externalReference = externalReference(),
      ),
    )
    rasMockServer.givenDeletionStatus(ca.externalReference!!.uuid, AppearanceDeletionStatus.NOT_SUPPORTED)

    val res = getAppearance(ca.id).successResponse<Appearance>()
    res.verifyAgainst(ca)
    assertThat(res.prison).isEqualTo(prison)
    assertThat(res.court).isEqualTo(court)
    assertThat(res.cancellable).isFalse
  }

  @Test
  fun `200 can retrieve court appearance with external reference and delete not permitted`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()
    val ca = givenCourtAppearance(
      courtAppearance(
        start = LocalDateTime.now().minusDays(2),
        prisonCode = prison.code,
        courtCode = court.code,
        externalReference = externalReference(),
      ),
    )

    val res = getAppearance(ca.id).successResponse<Appearance>()
    res.verifyAgainst(ca)
    assertThat(res.prison).isEqualTo(prison)
    assertThat(res.court).isEqualTo(court)
    assertThat(res.cancellable).isFalse
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
    assertThat(origin?.id).isEqualTo(ca.externalReference?.uuid)
    assertThat(origin?.source?.code).isEqualTo(ca.externalReference?.service?.code)
  }

  private fun getAppearance(
    id: UUID,
    role: String? = Roles.SCHEDULER_UI,
  ) = getAppearance(id.toString(), role)

  private fun getAppearance(
    id: String,
    role: String? = Roles.SCHEDULER_UI,
  ) = webTestClient
    .get()
    .uri(URL_TO_TEST, id)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/court-appearances/{id}"
  }
}
