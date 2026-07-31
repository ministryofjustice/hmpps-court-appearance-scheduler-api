package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.CourterRegisterExtension.Companion.courtRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes.Clash
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes.ClashPersonIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes.ClashRange
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes.ClashRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes.ClashResponse
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.RetrieveClashes
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.SequencedSet

class ClashesIntTest(
  @Autowired private val caOps: CourtAppearanceOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by caOps {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(CLASHES_URL, prisonCode())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getClashes(
      clashRequest(),
      Roles.allExcept(Roles.SCHEDULE_CLASHES_RO).toList(),
    ).expectStatus().isForbidden
  }

  @ParameterizedTest
  @MethodSource("badRequests")
  fun `400 bad request if request does not include at least one person identifier and one clash range`(request: ClashRequest) {
    getClashes(request).expectStatus().isBadRequest
  }

  @ParameterizedTest
  @MethodSource("clashes")
  fun `200 ok - can detect clashes`(range: ClashRange) {
    val court = courtRegister.givenCourt()
    val appearance = givenCourtAppearance(
      courtAppearance(
        start = CLASH_DATE.atTime(10, 0),
        end = CLASH_DATE.atTime(17, 0),
        courtCode = court.code,
      ),
    )

    val res = getClashes(
      clashRequest(
        personIdentifiers = linkedSetOf(clashIdentifier(appearance.person.identifier)),
        ranges = linkedSetOf(range),
      ),
    ).successResponse<ClashResponse>()

    assertThat(res.origin).isEqualTo(RetrieveClashes.ORIGIN)
    val personClash = res.data.single()
    assertThat(personClash.personIdentifier.value).isEqualTo(appearance.person.identifier)
    val clash = personClash.clashes.single()
    assertThat(clash.start).isEqualTo(appearance.start)
    assertThat(clash.end).isEqualTo(appearance.end)
    assertThat(clash.description).isEqualTo(
      Clash.Description(
        appearance.reason.description,
        if (appearance.reason.external) "Court appearance" else "Video link",
      ),
    )
    assertThat(clash.location.description).isEqualTo(court.name)
    assertThat(clash.additionalInformation).isEqualTo(Clash.AdditionalInformation(appearance.courtCode))
  }

  @ParameterizedTest
  @MethodSource("noClashes")
  fun `200 ok - returns empty when no clashes`(range: ClashRange) {
    val court = courtRegister.givenCourt()
    val appearance = givenCourtAppearance(
      courtAppearance(
        start = CLASH_DATE.atTime(10, 0),
        end = CLASH_DATE.atTime(17, 0),
        courtCode = court.code,
      ),
    )

    val res = getClashes(
      clashRequest(
        personIdentifiers = linkedSetOf(clashIdentifier(appearance.person.identifier)),
        ranges = linkedSetOf(range),
      ),
    ).successResponse<ClashResponse>()

    assertThat(res.origin).isEqualTo(RetrieveClashes.ORIGIN)
    assertThat(res.data).isEmpty()
  }

  private fun getClashes(
    request: ClashRequest,
    roles: List<String> = listOf(Roles.SCHEDULE_CLASHES_RO),
  ) = webTestClient
    .post()
    .uri(CLASHES_URL)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = roles))
    .exchange()

  companion object {
    const val CLASHES_URL = "/search/people/clashes"
    val CLASH_DATE: LocalDate = LocalDate.now().plusDays(7)

    private fun clashIdentifier(personIdentifier: String = personIdentifier()) = ClashPersonIdentifier(ClashPersonIdentifier.Type.PRISON_NUMBER, personIdentifier)

    private fun clashRange(
      start: LocalDateTime = CLASH_DATE.atTime(10, 0),
      end: LocalDateTime = CLASH_DATE.atTime(16, 0),
    ) = ClashRange(start, end)

    private fun clashRequest(
      personIdentifiers: SequencedSet<ClashPersonIdentifier> = linkedSetOf(clashIdentifier()),
      ranges: SequencedSet<ClashRange> = linkedSetOf(clashRange()),
    ) = ClashRequest(personIdentifiers, ranges)

    @JvmStatic
    fun clashes() = listOf(
      clashRange(start = CLASH_DATE.atTime(7, 0), end = CLASH_DATE.atTime(12, 0)),
      clashRange(start = CLASH_DATE.atTime(7, 0), end = CLASH_DATE.atTime(19, 0)),
      clashRange(start = CLASH_DATE.atTime(10, 0), end = CLASH_DATE.atTime(16, 0)),
      clashRange(start = CLASH_DATE.atTime(16, 0), end = CLASH_DATE.atTime(22, 0)),
    )

    @JvmStatic
    fun noClashes() = listOf(
      clashRange(start = CLASH_DATE.atTime(6, 0), end = CLASH_DATE.atTime(8, 0)),
      clashRange(start = CLASH_DATE.atTime(17, 0), end = CLASH_DATE.atTime(19, 0)),
      with(CLASH_DATE.plusDays(1)) {
        clashRange(this.atTime(8, 0), end = this.atTime(17, 0))
      },
      with(CLASH_DATE.minusDays(1)) {
        clashRange(this.atTime(8, 0), end = this.atTime(17, 0))
      },
    )

    @JvmStatic
    fun badRequests() = listOf(
      clashRequest(personIdentifiers = linkedSetOf(), ranges = linkedSetOf()),
      clashRequest(personIdentifiers = linkedSetOf(clashIdentifier()), ranges = linkedSetOf()),
      clashRequest(personIdentifiers = linkedSetOf(), ranges = linkedSetOf(clashRange())),
    )
  }
}
