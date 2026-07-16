package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.reconciliation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReconciliationHistoryRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearanceReconcileActive
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearanceReconcileEnhanced
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
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonApiMockServer.Companion.prisonerMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonRegisterMockServer.Companion.prison
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerApiExtension.Companion.prisonApi
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.RemandAndSentencingExtension.Companion.rasMockServer
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.schedule
import java.time.LocalDate

class ReconciliationIntTest(
  @Autowired pso: PersonSummaryOperations,
  @Autowired cao: CourtAppearanceOperations,
  @Autowired private val rhr: ReconciliationHistoryRepository,
) : IntegrationTest(),
  PersonSummaryOperations by pso,
  CourtAppearanceOperations by cao {
  @Test
  fun `active prisoner reconciliation is triggered successfully`() {
    val prisons = prisonRegister.givenNamedPrisons(setOf(prison(), prison(), prison()))
    val prisoners = prisons.flatMapIndexed { index, prison ->
      prisonerSearch.givenPrisonersAt(prison.code, (index + 1) * 5)
    }
    assertThat(prisoners).hasSize(30)
    prisoners.forEach { rasMockServer.givenReconciliationAppearances(it.prisonerNumber, emptyList()) }

    // trigger it twice to simulate multiple pods
    trigger.activeReconciliation()
    trigger.activeReconciliation()

    prisons.forEach { verify(prisonReconciliation, timeout(1000).times(1)).reconcile(it.code) }
    prisoners.forEach { verify(personReconciliation, timeout(1000).times(1)).reconcile(it.prisonerNumber) }

    val rec = rhr.findAll().single { it.type == CourtAppearanceReconcileActive.EVENT_TYPE }
    assertThat(rec.type).isEqualTo(CourtAppearanceReconcileActive.EVENT_TYPE)
    assertThat(rec.requestedOn).isEqualTo(LocalDate.now())
    assertThat(rec.version).isEqualTo(0)
  }

  @Test
  fun `enhanced reconciliation is triggered successfully`() {
    givenPersonSummary(personSummary())
    val people = findAllPeople()
    people.forEach { rasMockServer.givenReconciliationAppearances(it.identifier, emptyList()) }

    // trigger it twice to simulate multiple pods
    trigger.enhancedReconciliation()
    trigger.enhancedReconciliation()

    people.forEach { verify(personReconciliation, timeout(1000).times(1)).reconcile(it.identifier) }

    val rec = rhr.findAll().single { it.type == CourtAppearanceReconcileEnhanced.EVENT_TYPE }
    assertThat(rec.type).isEqualTo(CourtAppearanceReconcileEnhanced.EVENT_TYPE)
    assertThat(rec.requestedOn).isEqualTo(LocalDate.now())
    assertThat(rec.version).isEqualTo(0)
  }

  @Test
  fun `matching reconciliation results does not send telemetry events`() {
    val casAppearance = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    rasMockServer.givenReconciliationAppearances(casAppearance.person.identifier, listOf(casAppearance.schedule(false)))
    prisonApi.givenMovementsFor(
      casAppearance.person.identifier,
      listOf(
        prisonerMovement(toAgency = prisonCode(), dateTime = casAppearance.start.minusDays(2)),
        prisonerMovement(toAgency = casAppearance.prisonCode, dateTime = casAppearance.start.minusDays(1)),
        prisonerMovement(toAgency = prisonCode(), dateTime = casAppearance.start.plusDays(1)),
      ),
    )

    personReconciliation.reconcile(casAppearance.person.identifier)

    verify(telemetryClient, never()).trackEvent(any(), any(), any())
  }

  @Test
  fun `OUT used when no movement before start`() {
    val casAppearance =
      givenCourtAppearance(courtAppearance(prisonCode = "OUT", externalReference = externalReference()))
    rasMockServer.givenReconciliationAppearances(casAppearance.person.identifier, listOf(casAppearance.schedule(false)))
    prisonApi.givenMovementsFor(
      casAppearance.person.identifier,
      listOf(
        prisonerMovement(toAgency = casAppearance.prisonCode, dateTime = casAppearance.start.plusHours(1)),
        prisonerMovement(toAgency = prisonCode(), dateTime = casAppearance.start.plusDays(1)),
      ),
    )

    personReconciliation.reconcile(casAppearance.person.identifier)

    verify(telemetryClient, never()).trackEvent(any(), any(), any())
  }

  @Test
  fun `missing ids are identified`() {
    val casAppearance = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    val rasAppearance = rasMockServer.givenReconciliationAppearances(
      casAppearance.person.identifier,
      listOf(casAppearance.schedule(false).copy(id = newUuid())),
    ).single()

    rasMockServer.givenCourtAppearanceSchedules(emptyList())
    prisonApi.givenMovementsFor(casAppearance.person.identifier, emptyList())

    personReconciliation.reconcile(casAppearance.person.identifier)

    verify(telemetryClient).trackEvent(
      "Overall Count Mismatch",
      mapOf(
        "personIdentifier" to casAppearance.person.identifier,
        "casCount" to "1",
        "rasCount" to "1",
        "casMissing" to listOf(rasAppearance.id).joinToString(", "),
        "rasMissing" to listOf(casAppearance.externalReference!!.uuid).joinToString(", "),
      ),
      mapOf(),
    )
  }

  @Test
  fun `missing ids found on another person are identified`() {
    val casAppearance = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    val casFound = givenCourtAppearance(courtAppearance(prisonCode = casAppearance.prisonCode, externalReference = externalReference()))
    rasMockServer.givenReconciliationAppearances(
      casAppearance.person.identifier,
      listOf(
        casFound.schedule(false).copy(personIdentifier = casAppearance.person.identifier),
      ),
    )

    val rasFound = rasMockServer.givenCourtAppearanceSchedule(casAppearance.schedule(false).copy(personIdentifier = personIdentifier()))
    prisonApi.givenMovementsFor(casAppearance.person.identifier, listOf(prisonerMovement(casAppearance.prisonCode)))

    personReconciliation.reconcile(casAppearance.person.identifier)

    verify(telemetryClient).trackEvent(
      "Person Identifier Mismatch",
      mapOf(
        "personIdentifier" to casAppearance.person.identifier,
        "otherIdentifiers" to listOf(casFound.person.identifier, rasFound.personIdentifier).joinToString(", "),
      ),
      mapOf(),
    )
  }

  @Test
  fun `differing properties are identified`() {
    val casAppearance = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    val rasAppearance = rasMockServer.givenReconciliationAppearances(
      casAppearance.person.identifier,
      listOf(
        casAppearance.schedule(false)
          .copy(courtCode = courtCode(), start = casAppearance.start.plusHours(2), comments = word(30)),
      ),
    ).single()
    prisonApi.givenMovementsFor(casAppearance.person.identifier, emptyList())

    personReconciliation.reconcile(casAppearance.person.identifier)

    listOf("prisonCode", "courtCode", "start", "comments").forEach {
      verify(telemetryClient).trackEvent(
        "Property Mismatch",
        mapOf(
          "personIdentifier" to casAppearance.person.identifier,
          "propertyName" to it,
          "casId" to "${casAppearance.id}",
          "rasId" to "${rasAppearance.id}",
        ),
        mapOf(),
      )
    }
  }
}
