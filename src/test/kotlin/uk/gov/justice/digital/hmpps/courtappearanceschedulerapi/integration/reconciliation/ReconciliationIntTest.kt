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
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.externalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonApiMockServer.Companion.prisonerMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonRegisterMockServer.Companion.prison
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerApiExtension.Companion.prisonApi
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.RemandAndSentencingExtension.Companion.rasMockServer
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.schedule
import java.time.LocalDate

class ReconciliationIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired private val rhr: ReconciliationHistoryRepository,
) : IntegrationTest(),
  CourtAppearanceOperations by cao {
  @Test
  fun `reconciliation is triggered successfully`() {
    val prisons = prisonRegister.givenNamedPrisons(setOf(prison(), prison(), prison()))
    val prisoners = prisons.flatMapIndexed { index, prison ->
      prisonerSearch.givenPrisonersAt(prison.code, (index + 1) * 5)
    }
    assertThat(prisoners).hasSize(30)
    prisoners.forEach { rasMockServer.givenReconciliationAppearances(it.prisonerNumber, emptyList()) }

    trigger.activeReconciliation()
    trigger.activeReconciliation()

    prisons.forEach { verify(prisonReconciliation, timeout(1000).times(1)).reconcile(it.code) }
    prisoners.forEach { verify(personReconciliation, timeout(1000).times(1)).reconcile(it.prisonerNumber) }

    val rec = rhr.findAll().single()
    assertThat(rec.type).isEqualTo(CourtAppearanceReconcileActive.EVENT_TYPE)
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
  fun `missing ids are identified`() {
    val casAppearance = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    val rasAppearance = rasMockServer.givenReconciliationAppearances(
      casAppearance.person.identifier,
      listOf(casAppearance.schedule(false).copy(id = newUuid())),
    ).single()

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
