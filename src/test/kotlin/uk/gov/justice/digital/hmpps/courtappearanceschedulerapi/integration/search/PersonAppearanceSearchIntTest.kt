package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.CourtRegisterMockServer.Companion.court
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.CourterRegisterExtension.Companion.courtRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.CourtAppearanceSearchResponse
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.PersonAppearanceSearchRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PersonAppearanceSearchIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired pso: PersonSummaryOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao,
  PersonSummaryOperations by pso {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(URL_TO_TEST, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.SCHEDULER_RO, Roles.SCHEDULER_RW])
  fun `403 forbidden without correct role`(role: String) {
    searchAppearances(personIdentifier(), searchRequest(), role).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request if start after end`() {
    val res =
      searchAppearances(personIdentifier(), searchRequest(end = LocalDate.now(), start = LocalDate.now().plusDays(1)))
        .errorResponse(HttpStatus.BAD_REQUEST)

    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: End must be after start.")
  }

  @Test
  fun `appearances by person identifier`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()

    val toFind = givenCourtAppearance(courtAppearance(prisonCode = prison.code, courtCode = court.code))
    givenCourtAppearance(courtAppearance(prisonCode = prison.code, courtCode = court.code))

    val res = searchAppearances(toFind.person.identifier, searchRequest())
      .successResponse<CourtAppearanceSearchResponse>()

    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    assertThat(res.content.single().id).isEqualTo(toFind.id)
  }

  @Test
  fun `unscheduled not found by default`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()

    val scheduled = givenCourtAppearance(courtAppearance(prisonCode = prison.code, courtCode = court.code))
    val unscheduled = givenCourtAppearance(courtAppearance(prisonCode = prison.code, courtCode = court.code, unschedule = true))
    assertThat(unscheduled.status.code).isEqualTo(CourtAppearanceStatus.Code.UNSCHEDULED)

    val res = searchAppearances(scheduled.person.identifier, searchRequest())
      .successResponse<CourtAppearanceSearchResponse>()

    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    assertThat(res.content.single().id).isEqualTo(scheduled.id)
  }

  @Test
  fun `can filter appearances by court code`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()

    val toFind = givenCourtAppearance(courtAppearance(prisonCode = prison.code, courtCode = court.code))
    givenCourtAppearance(courtAppearance(prisonCode = prison.code))

    val res = searchAppearances(toFind.person.identifier, searchRequest(courtCodes = setOf(court.code)))
      .successResponse<CourtAppearanceSearchResponse>()

    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    assertThat(res.content.single().id).isEqualTo(toFind.id)
  }

  @Test
  fun `can find appearances by date`() {
    val prison = prisonRegister.givenPrison()
    val courts = courtRegister.givenCourts(setOf(court(), court()))
    val person = givenPersonSummary(personSummary())
    val start = LocalDate.now().plusDays(2)
    val end = start.plusDays(3)

    val appearances = (0..5).map {
      val startDateTime = LocalDateTime.of(start.minusDays(1).plusDays(it.toLong()), LocalTime.of(10, 0))
      givenCourtAppearance(
        courtAppearance(
          prisonCode = prison.code,
          personIdentifier = person.identifier,
          start = startDateTime,
          reasonCode = if (it % 2 == 0) "VL" else "CRT",
          courtCode = if (it % 2 == 0) courts.last().code else courts.first().code,
        ),
      )
    }

    val res = searchAppearances(
      person.identifier,
      searchRequest(start = start, end = end),
    ).successResponse<CourtAppearanceSearchResponse>()
    assertThat(res.content).hasSize(4)
    assertThat(res.metadata.totalElements).isEqualTo(4)
    assertThat(res.content.map { it.id }).containsExactlyElementsOf(appearances.subList(1, 5).map { it.id })
  }

  @Test
  fun `can filter and sort appearances by status`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()
    val person = givenPersonSummary(personSummary())

    val expired = givenCourtAppearance(
      courtAppearance(
        prisonCode = prison.code,
        courtCode = court.code,
        personIdentifier = person.identifier,
        start = LocalDate.now().minusDays(1).atTime(9, 0),
      ),
    )
    assertThat(expired.status.code).isEqualTo(CourtAppearanceStatus.Code.EXPIRED)
    val scheduled = givenCourtAppearance(
      courtAppearance(
        prisonCode = prison.code,
        courtCode = court.code,
        personIdentifier = person.identifier,
        start = LocalDate.now().atTime(6, 0),
        end = null,
      ),
    )
    assertThat(scheduled.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)
    val inProgress = givenCourtAppearance(
      courtAppearance(
        prisonCode = prison.code,
        courtCode = court.code,
        personIdentifier = person.identifier,
        start = LocalDate.now().atTime(10, 0),
        end = LocalDate.now().atTime(23, 59),
        movements = listOf(movement(CourtAppearanceMovement.Direction.OUT)),
      ),
    )
    assertThat(inProgress.status.code).isEqualTo(CourtAppearanceStatus.Code.IN_PROGRESS)
    val completed = givenCourtAppearance(
      courtAppearance(
        prisonCode = prison.code,
        courtCode = court.code,
        personIdentifier = person.identifier,
        start = LocalDate.now().minusDays(1).atTime(10, 0),
        movements = listOf(
          movement(CourtAppearanceMovement.Direction.OUT, LocalDate.now().minusDays(1).atTime(8, 0)),
          movement(CourtAppearanceMovement.Direction.IN, LocalDate.now().minusDays(1).atTime(15, 0)),
        ),
      ),
    )
    assertThat(completed.status.code).isEqualTo(CourtAppearanceStatus.Code.COMPLETED)

    val allAsc = searchAppearances(
      person.identifier,
      searchRequest(start = LocalDate.now().minusDays(3), sort = "${CourtAppearance::status.name},asc"),
    ).successResponse<CourtAppearanceSearchResponse>()
    assertThat(allAsc.content).hasSize(4)
    assertThat(allAsc.metadata.totalElements).isEqualTo(4)
    assertThat(allAsc.content.map { it.id }).containsExactly(scheduled.id, inProgress.id, completed.id, expired.id)

    val allDesc = searchAppearances(
      person.identifier,
      searchRequest(start = LocalDate.now().minusDays(3), sort = "${CourtAppearance::status.name},desc"),
    ).successResponse<CourtAppearanceSearchResponse>()
    assertThat(allDesc.content).hasSize(4)
    assertThat(allDesc.metadata.totalElements).isEqualTo(4)
    assertThat(allDesc.content.map { it.id }).containsExactly(expired.id, completed.id, inProgress.id, scheduled.id)

    val schedOnly = searchAppearances(
      person.identifier,
      searchRequest(
        start = LocalDate.now().minusDays(3),
        statuses = setOf(
          CourtAppearanceStatus.Code.SCHEDULED,
        ),
      ),
    ).successResponse<CourtAppearanceSearchResponse>()

    assertThat(schedOnly.content).hasSize(1)
    assertThat(schedOnly.metadata.totalElements).isEqualTo(1)
    assertThat(schedOnly.content.map { it.id }).containsExactly(scheduled.id)

    val two = searchAppearances(
      person.identifier,
      searchRequest(
        start = LocalDate.now().minusDays(3),
        statuses = setOf(
          CourtAppearanceStatus.Code.COMPLETED,
          CourtAppearanceStatus.Code.EXPIRED,
        ),
      ),
    ).successResponse<CourtAppearanceSearchResponse>()

    assertThat(two.content).hasSize(2)
    assertThat(two.metadata.totalElements).isEqualTo(2)
    assertThat(two.content.map { it.id }).containsExactly(expired.id, completed.id)
  }

  @Test
  fun `can filter appearances by reason and external flag`() {
    val prison = prisonRegister.givenPrison()
    val court = courtRegister.givenCourt()
    val person = givenPersonSummary(personSummary())

    val videoAppearances = listOf(
      givenCourtAppearance(
        courtAppearance(
          prisonCode = prison.code,
          courtCode = court.code,
          personIdentifier = person.identifier,
          reasonCode = "VL",
          start = LocalDate.now().plusDays(1).atTime(9, 0),
        ),
      ),
      givenCourtAppearance(
        courtAppearance(
          prisonCode = prison.code,
          courtCode = court.code,
          personIdentifier = person.identifier,
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
          personIdentifier = person.identifier,
          reasonCode = "CRT",
          start = LocalDate.now().plusDays(1).atTime(9, 0),
        ),
      ),
      givenCourtAppearance(
        courtAppearance(
          prisonCode = prison.code,
          courtCode = court.code,
          personIdentifier = person.identifier,
          reasonCode = "CRT",
          start = LocalDate.now().plusDays(1).atTime(11, 0),
        ),
      ),
    )

    val byAllReasons = searchAppearances(person.identifier, searchRequest(reasons = setOf("VL", "CRT")))
      .successResponse<CourtAppearanceSearchResponse>()
    assertThat(byAllReasons.content).hasSize(4)
    assertThat(byAllReasons.metadata.totalElements).isEqualTo(4)

    val vlReasons = searchAppearances(person.identifier, searchRequest(reasons = setOf("VL")))
      .successResponse<CourtAppearanceSearchResponse>()
    assertThat(vlReasons.content).hasSize(2)
    assertThat(vlReasons.metadata.totalElements).isEqualTo(2)
    assertThat(vlReasons.content.map { it.id }).containsExactlyElementsOf(videoAppearances.map { it.id })

    val crtReasons = searchAppearances(person.identifier, searchRequest(reasons = setOf("CRT")))
      .successResponse<CourtAppearanceSearchResponse>()
    assertThat(crtReasons.content).hasSize(2)
    assertThat(crtReasons.metadata.totalElements).isEqualTo(2)
    assertThat(crtReasons.content.map { it.id }).containsExactlyElementsOf(inPersonAppearances.map { it.id })

    val externalAppearances = searchAppearances(person.identifier, searchRequest(external = true))
      .successResponse<CourtAppearanceSearchResponse>()
    assertThat(externalAppearances.content).hasSize(2)
    assertThat(externalAppearances.metadata.totalElements).isEqualTo(2)
    assertThat(externalAppearances.content.map { it.id }).containsExactlyElementsOf(inPersonAppearances.map { it.id })

    val internalAppearances = searchAppearances(person.identifier, searchRequest(external = false))
      .successResponse<CourtAppearanceSearchResponse>()
    assertThat(internalAppearances.content).hasSize(2)
    assertThat(internalAppearances.metadata.totalElements).isEqualTo(2)
    assertThat(internalAppearances.content.map { it.id }).containsExactlyElementsOf(videoAppearances.map { it.id })
  }

  private fun searchRequest(
    start: LocalDate = LocalDate.now(),
    end: LocalDate = start.plusDays(30),
    statuses: Set<CourtAppearanceStatus.Code> = emptySet(),
    reasons: Set<String> = emptySet(),
    courtCodes: Set<String> = emptySet(),
    external: Boolean? = null,
    page: Int = 1,
    size: Int = 10,
    sort: String = "${CourtAppearance::start.name},asc",
  ) = PersonAppearanceSearchRequest(start, end, statuses, reasons, courtCodes, external, page, size, sort)

  private fun searchAppearances(
    personIdentifier: String,
    request: PersonAppearanceSearchRequest,
    role: String? = Roles.SCHEDULER_UI,
  ): WebTestClient.ResponseSpec {
    check(personIdentifier.matches(Prisoner.PATTERN.toRegex())) {
      "Test error - person identifier does not match regex"
    }
    return webTestClient
      .post()
      .uri(URL_TO_TEST, personIdentifier)
      .bodyValue(request)
      .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
      .exchange()
  }

  companion object {
    const val URL_TO_TEST = "/search/people/{personIdentifier}/court-appearances"
  }
}
