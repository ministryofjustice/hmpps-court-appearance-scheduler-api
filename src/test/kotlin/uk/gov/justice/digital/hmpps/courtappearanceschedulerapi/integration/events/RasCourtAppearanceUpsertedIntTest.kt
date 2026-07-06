package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.events

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.PersonReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.RasAppearanceInformation
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.RasAppearanceInserted
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.RasAppearanceUpdated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.externalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonApiMockServer.Companion.prisonerMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerApiExtension.Companion.prisonApi
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.RemandAndSentencingExtension.Companion.rasMockServer
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.RemandAndSentencingMockServer.Companion.rasSchedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.schedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

class RasCourtAppearanceUpsertedIntTest(
  @Autowired pso: PersonSummaryOperations,
  @Autowired cao: CourtAppearanceOperations,
) : IntegrationTest(),
  PersonSummaryOperations by pso,
  CourtAppearanceOperations by cao {

  @Test
  fun `create new if record does not exist`() {
    val prisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode()))
    val ras = rasMockServer.givenCourtAppearanceSchedule(rasSchedule(prisoner.prisonerNumber))
    prisonApi.givenMovementsFor(ras.personIdentifier, listOf(prisonerMovement(prisoner.prisonId!!)))

    sendDomainEvent(event(ras.id, ras.personIdentifier))

    waitUntil { findByExternalReference(ras.externalReference) != null }

    val saved = requireNotNull(findByExternalReference(ras.externalReference))
    saved verifyAgainst ras
  }

  @Test
  fun `create new unscheduled if a duplicate is created`() {
    val person = givenPersonSummary(personSummary())
    val ras = rasMockServer.givenCourtAppearanceSchedule(rasSchedule(person.identifier, isDuplicate = true))
    prisonApi.givenMovementsFor(ras.personIdentifier, listOf(prisonerMovement()))

    sendDomainEvent(event(ras.id, ras.personIdentifier))

    waitUntil { findByExternalReference(ras.externalReference) != null }

    val saved = requireNotNull(findByExternalReference(ras.externalReference))
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.UNSCHEDULED)
    saved verifyAgainst ras
  }

  @Test
  fun `create new completed if start before latest movement`() {
    val person = givenPersonSummary(personSummary())
    val ras = rasMockServer.givenCourtAppearanceSchedule(
      rasSchedule(person.identifier, start = LocalDate.now().minusDays(7).atTime(10, 0)),
    )
    prisonApi.givenMovementsFor(
      ras.personIdentifier,
      listOf(prisonerMovement(dateTime = LocalDateTime.now())),
    )

    sendDomainEvent(event(ras.id, ras.personIdentifier))

    waitUntil { findByExternalReference(ras.externalReference) != null }

    val saved = requireNotNull(findByExternalReference(ras.externalReference))
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.COMPLETED)
    saved verifyAgainst ras
  }

  @Test
  fun `can update an existing appearance`() {
    val existing = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    assertThat(existing.external).isTrue
    val ras = rasMockServer.givenCourtAppearanceSchedule(
      existing.schedule(false).copy(
        courtCode = courtCode(),
        reason = CourtAppearanceSchedule.ScheduleReason("VL"),
        start = LocalDate.now().plusDays(14).atTime(10, 0),
        comments = word(30),
      ),
    )
    prisonApi.givenMovementsFor(ras.personIdentifier, listOf(prisonerMovement(existing.person.prisonCode!!)))

    sendDomainEvent(event(ras.id, ras.personIdentifier))

    waitUntil { findByExternalReference(ras.externalReference) != null }

    val saved = requireNotNull(findByExternalReference(ras.externalReference))
    assertThat(saved.external).isFalse
    saved verifyAgainst ras
  }

  @Test
  fun `can complete existing appearance if rescheduling before latest movement`() {
    val existing = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    val ras = rasMockServer.givenCourtAppearanceSchedule(
      existing.schedule(false).copy(
        start = LocalDate.now().minusDays(2).atTime(10, 0),
      ),
    )
    prisonApi.givenMovementsFor(ras.personIdentifier, listOf(prisonerMovement(existing.person.prisonCode!!)))

    sendDomainEvent(event(ras.id, ras.personIdentifier))

    waitUntil { findByExternalReference(ras.externalReference) != null }

    val saved = requireNotNull(findByExternalReference(ras.externalReference))
    assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.COMPLETED)
    saved verifyAgainst ras
  }
}

private fun event(
  appearanceId: UUID = UUID.randomUUID(),
  personIdentifier: String = personIdentifier(),
) = if (Random.nextBoolean()) {
  RasAppearanceInserted(RasAppearanceInformation(appearanceId), PersonReference.withIdentifier(personIdentifier))
} else {
  RasAppearanceUpdated(RasAppearanceInformation(appearanceId), PersonReference.withIdentifier(personIdentifier))
}

private infix fun CourtAppearance.verifyAgainst(ras: CourtAppearanceSchedule) {
  assertThat(person.identifier).isEqualTo(ras.personIdentifier)
  assertThat(courtCode).isEqualTo(ras.courtCode)
  assertThat(reason.code).isEqualTo(ras.reason.code)
  assertThat(start).isCloseTo(ras.start, within(1, ChronoUnit.MINUTES))
  assertThat(comments).isEqualTo(ras.comments)
  assertThat(status.code == CourtAppearanceStatus.Code.UNSCHEDULED).isEqualTo(ras.isDuplicate)
}
