package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.CaseloadIdHeader
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceScheduled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ScheduleCourtAppearance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleCourtAppearanceIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired pso: PersonSummaryOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao,
  PersonSummaryOperations by pso {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .post()
      .uri(URL_TO_TEST, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    scheduleAppearance(personIdentifier(), scheduleCourtAppearance(), role = Roles.SCHEDULER_RO)
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - cannot schedule a court appearance for a prisoner without a responsible prison`() {
    val prisonCode = prisonCode()
    val username = username()
    val person = givenPersonSummary(personSummary(prisonCode = null))

    val request = scheduleCourtAppearance(prisonCode, reasonCode = "CE")
    val res = scheduleAppearance(person.identifier, request, username, prisonCode)
      .errorResponse(HttpStatus.BAD_REQUEST)

    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Invalid request")
    assertThat(res.developerMessage).isEqualTo("Unable to schedule a court appearance when current location is unknown")
  }

  @Test
  fun `200 can schedule a court appearance for an existing prisoner`() {
    val prisonCode = prisonCode()
    val username = username()
    val person = givenPersonSummary(personSummary(prisonCode = prisonCode))

    val request = scheduleCourtAppearance(prisonCode, reasonCode = "CE")
    val res = scheduleAppearance(person.identifier, request, username, prisonCode).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtAppearance(res.id))
    saved.verifyAgainst(request)
    assertThat(saved.person.identifier).isEqualTo(person.identifier)
    assertThat(saved.prisonCode).isEqualTo(prisonCode)
    assertThat(saved.external).isTrue
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = username, caseloadId = prisonCode),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceScheduled(saved.person.identifier, saved.id, null).publication(saved.id)),
    )
  }

  @Test
  fun `200 can schedule a court appearance for a new prisoner`() {
    val prisonCode = prisonCode()
    val prisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode))
    val username = username()

    val request = scheduleCourtAppearance(prisonCode, reasonCode = "VLWT")
    val res = scheduleAppearance(prisoner.prisonerNumber, request, username, prisonCode).successResponse<ReferenceId>()

    val saved = requireNotNull(findCourtAppearance(res.id))
    saved.verifyAgainst(request)
    assertThat(saved.external).isFalse
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)
    with(saved.person) {
      assertThat(identifier).isEqualTo(prisoner.prisonerNumber)
      assertThat(firstName).isEqualTo(prisoner.firstName)
      assertThat(lastName).isEqualTo(prisoner.lastName)
      assertThat(this.prisonCode).isEqualTo(prisoner.lastPrisonId)
    }

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = username, caseloadId = prisonCode),
    )

    verifyEventPublications(
      saved,
      setOf(CourtAppearanceScheduled(saved.person.identifier, saved.id, null).publication(saved.id)),
    )
  }

  private fun CourtAppearance.verifyAgainst(request: ScheduleCourtAppearance) {
    assertThat(courtCode).isEqualTo(request.courtCode)
    assertThat(start).isEqualTo(request.start)
    assertThat(end).isEqualTo(request.end)
    assertThat(reason.code).isEqualTo(request.reasonCode)
    assertThat(comments).isEqualTo(request.comments)
    assertThat(external).isEqualTo(reason.external)
  }

  private fun scheduleCourtAppearance(
    courtCode: String = courtCode(),
    reasonCode: String = "CRT",
    start: LocalDateTime = LocalDateTime.of(LocalDate.now().plusDays(7), LocalTime.of(10, 0)),
    end: LocalDateTime? = LocalDateTime.of(LocalDate.now().plusDays(7), LocalTime.of(17, 0)),
    comments: String? = word(25),
  ) = ScheduleCourtAppearance(courtCode, reasonCode, start, end, comments)

  private fun scheduleAppearance(
    personIdentifier: String,
    request: ScheduleCourtAppearance,
    username: String = username(),
    caseloadId: String? = null,
    role: String? = listOf(Roles.SCHEDULER_RW, Roles.SCHEDULER_UI).random(),
  ) = webTestClient
    .post()
    .uri(URL_TO_TEST, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = username, roles = listOfNotNull(role)))
    .headers { hc -> caseloadId?.also { hc.put(CaseloadIdHeader.NAME, listOf(it)) } }
    .exchange()

  companion object {
    const val URL_TO_TEST = "/court-appearances/{personIdentifier}"
  }
}
