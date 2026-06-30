package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.reconciliation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.externalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonRegisterMockServer.Companion.prison
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.RemandAndSentencingExtension.Companion.rasMockServer
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.schedule

class ReconciliationIntTest(
  @Autowired cao: CourtAppearanceOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao {
  @Test
  fun `reconciliation is triggered successfully`() {
    val prisons = prisonRegister.givenNamedPrisons(setOf(prison(), prison(), prison()))
    val prisoners = prisons.flatMapIndexed { index, prison ->
      prisonerSearch.givenPrisonersAt(prison.code, (index + 1) * 5)
    }
    assertThat(prisoners).hasSize(30)
    prisoners.forEach { rasMockServer.givenReconciliationAppearances(it.prisonerNumber, listOf()) }

    trigger.reconciliation()

    prisons.forEach { verify(prisonReconciliation, timeout(1000).times(1)).reconcile(it.code) }
    prisoners.forEach { verify(personReconciliation, timeout(1000).times(1)).reconcile(it.prisonerNumber) }
  }

  @Test
  fun `matching reconciliation results does not send telemetry events`() {
    val casAppearance = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    rasMockServer.givenReconciliationAppearances(casAppearance.person.identifier, listOf(casAppearance.schedule(false)))

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

    personReconciliation.reconcile(casAppearance.person.identifier)

    verify(telemetryClient).trackEvent(
      "Property Mismatch",
      mapOf(
        "personIdentifier" to casAppearance.person.identifier,
        "casId" to "${casAppearance.id}",
        "rasId" to "${rasAppearance.id}",
        "casPropertyName" to "courtCode",
        "rasPropertyName" to "courtCode",
      ),
      mapOf(),
    )

    verify(telemetryClient).trackEvent(
      "Property Mismatch",
      mapOf(
        "personIdentifier" to casAppearance.person.identifier,
        "casId" to "${casAppearance.id}",
        "rasId" to "${rasAppearance.id}",
        "casPropertyName" to "start",
        "rasPropertyName" to "start",
      ),
      mapOf(),
    )

    verify(telemetryClient).trackEvent(
      "Property Mismatch",
      mapOf(
        "personIdentifier" to casAppearance.person.identifier,
        "casId" to "${casAppearance.id}",
        "rasId" to "${rasAppearance.id}",
        "casPropertyName" to "comments",
        "rasPropertyName" to "comments",
      ),
      mapOf(),
    )
  }
}
