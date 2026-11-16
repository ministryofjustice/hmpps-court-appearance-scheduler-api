package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.replication

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Prison
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.replication.IdSearchRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.replication.IdSearchResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReplicationSearchIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired pso: PersonSummaryOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao,
  PersonSummaryOperations by pso {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(URL_TO_TEST)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.SCHEDULER_UI, Roles.NOMIS_SYNC, Roles.SCHEDULE_CLASHES_RO, Roles.SCHEDULER_RO, Roles.SCHEDULER_RW])
  fun `403 forbidden without correct role`(role: String) {
    searchAppearances(searchRequest(), role).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request if end is before start`() {
    val res = searchAppearances(searchRequest(start = LocalDate.now().plusDays(1), end = LocalDate.now().minusDays(1)))
      .errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: End must be after start.")
  }

  @Test
  fun `400 bad request if prison code does not match pattern`() {
    val res = searchAppearances(searchRequest(prisonCode = "unknown")).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: Prison code should match `${Prison.PATTERN}`.")
  }

  @Test
  fun `400 bad request if person identifier does not match pattern`() {
    val res = searchAppearances(searchRequest(personIdentifier = "unknown")).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: Person identifier should match `${Prisoner.PATTERN}`.")
  }

  @Test
  fun `200 ok - returns empty when no appearances match`() {
    val request = searchRequest(start = LocalDate.now().minusDays(90), end = LocalDate.now().minusDays(60))
    val res = searchAppearances(request).successResponse<IdSearchResponse>()
    assertThat(res.content).isEmpty()
    assertThat(res.metadata.totalElements).isEqualTo(0)
  }

  @Test
  fun `can find ids for date range`() {
    val start = LocalDate.now().plusDays(60)
    val end = start.plusDays(6)
    val appearances = (0..8).map {
      val startDateTime = LocalDateTime.of(start.minusDays(1).plusDays(it.toLong()), LocalTime.of(10, 0))
      givenCourtAppearance(courtAppearance(start = startDateTime))
    }
    val request = searchRequest(start = start, end = end)
    val res = searchAppearances(request).successResponse<IdSearchResponse>()
    assertThat(res.metadata.totalElements).isEqualTo(7)
    assertThat(res.content.map { it.id }).containsExactlyElementsOf(appearances.subList(1, 8).map { it.id })
  }

  @Test
  fun `can find ids by person identifier`() {
    val personToFind = givenPersonSummary(personSummary())
    val toFind = givenCourtAppearance(courtAppearance(personIdentifier = personToFind.identifier))
    val personNotFound = givenPersonSummary(personSummary())
    givenCourtAppearance(courtAppearance(personIdentifier = personNotFound.identifier))

    val res = searchAppearances(searchRequest(personIdentifier = personToFind.identifier)).successResponse<IdSearchResponse>()
    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    assertThat(res.content.single().id).isEqualTo(toFind.id)
  }

  @Test
  fun `can find ids by prisonCode`() {
    val personToFind = givenPersonSummary(personSummary())
    val toFind = givenCourtAppearance(courtAppearance(personIdentifier = personToFind.identifier))
    val personNotFound = givenPersonSummary(personSummary())
    givenCourtAppearance(courtAppearance(personIdentifier = personNotFound.identifier))

    val res = searchAppearances(searchRequest(prisonCode = personToFind.prisonCode)).successResponse<IdSearchResponse>()
    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    assertThat(res.content.single().id).isEqualTo(toFind.id)
  }

  private fun searchRequest(
    start: LocalDate = LocalDate.now().minusDays(5),
    end: LocalDate = LocalDate.now().plusDays(10),
    prisonCode: String? = null,
    personIdentifier: String? = null,
    page: Int = 1,
    size: Int = 10,
  ) = IdSearchRequest(start, end, prisonCode, personIdentifier, page, size)

  private fun searchAppearances(
    request: IdSearchRequest,
    role: String? = Roles.SCHEDULE_REPLICATION_RO,
  ) = webTestClient
    .get()
    .uri { builder ->
      builder.path(URL_TO_TEST)
        .queryParam("start", request.start)
        .queryParam("end", request.end)
        .queryParam("page", request.page)
        .queryParam("size", request.size)

      request.prisonCode?.also { builder.queryParam("prisonCode", it) }
      request.personIdentifier?.also { builder.queryParam("personIdentifier", it) }

      builder.build()
    }
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/schedule-replication/court-appearance-ids"
  }
}
