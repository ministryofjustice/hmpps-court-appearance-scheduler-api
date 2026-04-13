package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.CourtRegisterMockServer.Companion.court
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.CourterRegisterExtension.Companion.courtRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.AppearanceScheduleSearchRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.CourtAppearanceSchedules
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AppearanceScheduleSearchIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired pso: PersonSummaryOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao,
  PersonSummaryOperations by pso {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(URL_TO_TEST, "NAR")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.SCHEDULER_UI])
  fun `403 forbidden without correct role`(role: String) {
    val prisonCode = "FBD"
    searchAppearances(prisonCode, searchRequest(), role).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request if date range more than 31 days`() {
    val prisonCode = "IDR"
    val res = searchAppearances(prisonCode, searchRequest(start = LocalDate.now(), end = LocalDate.now().plusDays(32)))
      .errorResponse(HttpStatus.BAD_REQUEST)

    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: Invalid date range")
  }

  @Test
  fun `can find appearances by date`() {
    val prison = prisonRegister.givenPrison()
    val courts = courtRegister.givenCourts(setOf(court(), court()))
    val start = LocalDate.now().plusDays(2)
    val end = start.plusDays(3)

    val appearances = (0..5).map {
      val startDateTime = LocalDateTime.of(start.minusDays(1).plusDays(it.toLong()), LocalTime.of(10, 0))
      givenCourtAppearance(
        courtAppearance(
          prisonCode = prison.code,
          start = startDateTime,
          reasonCode = if (it % 2 == 0) "VL" else "CRT",
          courtCode = if (it % 2 == 0) courts.last().code else courts.first().code,
        ),
      )
    }

    val res = searchAppearances(
      prison.code,
      searchRequest(start = start, end = end),
    ).successResponse<CourtAppearanceSchedules>()
    assertThat(res.content).hasSize(4)
    assertThat(res.metadata.totalElements).isEqualTo(4)
    println(appearances.map { it.start })
    println(res.content.map { it.start })
    assertThat(res.content.map { it.id }).containsExactlyElementsOf(appearances.subList(1, 5).map { it.id })
  }

  @Test
  fun `can filter appearances by person identifier`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()

    val toFind = givenCourtAppearance(courtAppearance(prisonCode = prison.code, courtCode = court.code))
    givenCourtAppearance(courtAppearance(prisonCode = prison.code, courtCode = court.code))

    val res = searchAppearances(prison.code, searchRequest(personIdentifiers = setOf(toFind.person.identifier)))
      .successResponse<CourtAppearanceSchedules>()

    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    assertThat(res.content.single().id).isEqualTo(toFind.id)
  }

  @Test
  fun `can filter and sort appearances by status`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()

    val expired = givenCourtAppearance(
      courtAppearance(
        prisonCode = prison.code,
        courtCode = court.code,
        start = LocalDate.now().minusDays(1).atTime(9, 0),
      ),
    )
    val scheduled = givenCourtAppearance(courtAppearance(prisonCode = prison.code, courtCode = court.code))
    val inProgress = givenCourtAppearance(
      courtAppearance(
        prisonCode = prison.code,
        courtCode = court.code,
        start = LocalDate.now().atTime(10, 0),
        movements = listOf(movement(CourtAppearanceMovement.Direction.OUT)),
      ),
    )
    val completed = givenCourtAppearance(
      courtAppearance(
        prisonCode = prison.code,
        courtCode = court.code,
        start = LocalDate.now().minusDays(1).atTime(10, 0),
        movements = listOf(
          movement(CourtAppearanceMovement.Direction.OUT, LocalDate.now().minusDays(1).atTime(8, 0)),
          movement(CourtAppearanceMovement.Direction.IN, LocalDate.now().minusDays(1).atTime(15, 0)),
        ),
      ),
    )

    val allAsc = searchAppearances(
      prison.code,
      searchRequest(start = LocalDate.now().minusDays(3), sort = "${CourtAppearance::status.name},asc"),
    ).successResponse<CourtAppearanceSchedules>()
    assertThat(allAsc.content).hasSize(4)
    assertThat(allAsc.metadata.totalElements).isEqualTo(4)
    assertThat(allAsc.content.map { it.id }).containsExactly(scheduled.id, inProgress.id, completed.id, expired.id)

    val allDesc = searchAppearances(
      prison.code,
      searchRequest(start = LocalDate.now().minusDays(3), sort = "${CourtAppearance::status.name},desc"),
    ).successResponse<CourtAppearanceSchedules>()
    assertThat(allDesc.content).hasSize(4)
    assertThat(allDesc.metadata.totalElements).isEqualTo(4)
    assertThat(allDesc.content.map { it.id }).containsExactly(expired.id, completed.id, inProgress.id, scheduled.id)

    val schedOnly = searchAppearances(
      prison.code,
      searchRequest(
        start = LocalDate.now().minusDays(3),
        statuses = setOf(
          CourtAppearanceStatus.Code.SCHEDULED,
        ),
      ),
    ).successResponse<CourtAppearanceSchedules>()

    assertThat(schedOnly.content).hasSize(1)
    assertThat(schedOnly.metadata.totalElements).isEqualTo(1)
    assertThat(schedOnly.content.map { it.id }).containsExactly(scheduled.id)

    val two = searchAppearances(
      prison.code,
      searchRequest(
        start = LocalDate.now().minusDays(3),
        statuses = setOf(
          CourtAppearanceStatus.Code.COMPLETED,
          CourtAppearanceStatus.Code.EXPIRED,
        ),
      ),
    ).successResponse<CourtAppearanceSchedules>()

    assertThat(two.content).hasSize(2)
    assertThat(two.metadata.totalElements).isEqualTo(2)
    assertThat(two.content.map { it.id }).containsExactly(expired.id, completed.id)
  }

  @Test
  fun `can filter appearances by reason and external flag`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()

    val videoAppearances = listOf(
      givenCourtAppearance(
        courtAppearance(
          prisonCode = prison.code,
          courtCode = court.code,
          reasonCode = "VL",
          start = LocalDate.now().plusDays(1).atTime(9, 0),
        ),
      ),
      givenCourtAppearance(
        courtAppearance(
          prisonCode = prison.code,
          courtCode = court.code,
          reasonCode = "VL",
          start = LocalDate.now().plusDays(1).atTime(11, 0),
        ),
      ),
    )
    val inPersonAppearances = listOf(
      givenCourtAppearance(
        courtAppearance(
          prisonCode = prison.code,
          courtCode = court.code,
          reasonCode = "CRT",
          start = LocalDate.now().plusDays(1).atTime(9, 0),
        ),
      ),
      givenCourtAppearance(
        courtAppearance(
          prisonCode = prison.code,
          courtCode = court.code,
          reasonCode = "CRT",
          start = LocalDate.now().plusDays(1).atTime(11, 0),
        ),
      ),
    )

    val byAllReasons = searchAppearances(prison.code, searchRequest(reasons = setOf("VL", "CRT")))
      .successResponse<CourtAppearanceSchedules>()
    assertThat(byAllReasons.content).hasSize(4)
    assertThat(byAllReasons.metadata.totalElements).isEqualTo(4)

    val vlReasons = searchAppearances(prison.code, searchRequest(reasons = setOf("VL")))
      .successResponse<CourtAppearanceSchedules>()
    assertThat(vlReasons.content).hasSize(2)
    assertThat(vlReasons.metadata.totalElements).isEqualTo(2)
    assertThat(vlReasons.content.map { it.id }).containsExactlyElementsOf(videoAppearances.map { it.id })

    val crtReasons = searchAppearances(prison.code, searchRequest(reasons = setOf("CRT")))
      .successResponse<CourtAppearanceSchedules>()
    assertThat(crtReasons.content).hasSize(2)
    assertThat(crtReasons.metadata.totalElements).isEqualTo(2)
    assertThat(crtReasons.content.map { it.id }).containsExactlyElementsOf(inPersonAppearances.map { it.id })

    val externalAppearances = searchAppearances(prison.code, searchRequest(external = true))
      .successResponse<CourtAppearanceSchedules>()
    assertThat(externalAppearances.content).hasSize(2)
    assertThat(externalAppearances.metadata.totalElements).isEqualTo(2)
    assertThat(externalAppearances.content.map { it.id }).containsExactlyElementsOf(inPersonAppearances.map { it.id })

    val internalAppearances = searchAppearances(prison.code, searchRequest(external = false))
      .successResponse<CourtAppearanceSchedules>()
    assertThat(internalAppearances.content).hasSize(2)
    assertThat(internalAppearances.metadata.totalElements).isEqualTo(2)
    assertThat(internalAppearances.content.map { it.id }).containsExactlyElementsOf(videoAppearances.map { it.id })
  }

  @Test
  fun `person not resident is not found`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()
    val anotherPrisonCode = prisonCode()

    val fPerson = givenPersonSummary(personSummary(prisonCode = prison.code))
    val nfPerson = givenPersonSummary(personSummary(prisonCode = anotherPrisonCode))

    val fApp = givenCourtAppearance(
      courtAppearance(
        prisonCode = prison.code,
        courtCode = court.code,
        personIdentifier = fPerson.identifier,
      ),
    )
    givenCourtAppearance(
      courtAppearance(
        prisonCode = prison.code,
        courtCode = court.code,
        personIdentifier = nfPerson.identifier,
      ),
    )

    val res = searchAppearances(prison.code, searchRequest()).successResponse<CourtAppearanceSchedules>()
    assertThat(res.content.size).isEqualTo(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    with(res.content.single()) {
      assertThat(personIdentifier).isEqualTo(fPerson.identifier)
      assertThat(id).isEqualTo(fApp.id)
    }
  }

  private fun searchRequest(
    personIdentifiers: Set<String> = emptySet(),
    start: LocalDate = LocalDate.now(),
    end: LocalDate = start.plusDays(30),
    statuses: Set<CourtAppearanceStatus.Code> = emptySet(),
    reasons: Set<String> = emptySet(),
    external: Boolean? = null,
    page: Int = 1,
    size: Int = 10,
    sort: String = "${CourtAppearance::start.name},asc",
  ) = AppearanceScheduleSearchRequest(personIdentifiers, start, end, statuses, reasons, external, page, size, sort)

  private fun searchAppearances(
    prisonCode: String,
    request: AppearanceScheduleSearchRequest,
    role: String? = listOf(Roles.SCHEDULER_RO, Roles.SCHEDULER_RW).random(),
  ) = webTestClient
    .post()
    .uri(URL_TO_TEST, prisonCode)
    .bodyValue(request)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/search/prisons/{prisonCode}/court-appearances/schedules"
  }
}
