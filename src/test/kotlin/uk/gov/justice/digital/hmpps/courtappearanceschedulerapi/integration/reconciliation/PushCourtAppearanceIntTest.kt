package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.reconciliation

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.externalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.asUpdateRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.RemandAndSentencingExtension.Companion.rasMockServer
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.schedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation.PushPersonAppearanceData

class PushCourtAppearanceIntTest(
  @Autowired pso: PersonSummaryOperations,
  @Autowired cao: CourtAppearanceOperations,
  @Autowired private val ppa: PushPersonAppearanceData,
) : IntegrationTest(),
  PersonSummaryOperations by pso,
  CourtAppearanceOperations by cao {

  @Test
  fun `can successfully push all ras appearances to remand and sentencing`() {
    val person = givenPersonSummary(personSummary())
    val reasonCodes = listOf("CRT", "CA", "VL", "VLAP", "CE", "19", "22", "VLPC", "VLPD", "VLPP")
    val rasAppearances = (1..10).map {
      givenCourtAppearance(
        courtAppearance(
          person.identifier,
          reasonCode = reasonCodes.random(),
          externalReference = externalReference(),
        ),
      )
    }
    (1..10).forEach { _ ->
      givenCourtAppearance(
        courtAppearance(
          person.identifier,
          reasonCode = reasonCodes.random(),
          externalReference = null,
        ),
      )
    }

    rasMockServer.givenReconciliationAppearances(
      person.identifier,
      rasAppearances.map {
        it.schedule(false)
          .copy(comments = null, reason = CourtAppearanceSchedule.ScheduleReason(reasonCodes.random()))
      },
    )

    val pushRequests = rasAppearances.map { it.externalReference!!.uuid to it.asUpdateRequest() }
    pushRequests.forEach { rasMockServer.givenSuccessfulUpdate(it.first, it.second) }

    ppa.toRemandAndSentencing(person.identifier)

    verify(rasClient, timeout(1000).times(10)).updateCourtAppearanceSchedule(any(), any())
    pushRequests.forEach { verify(rasClient).updateCourtAppearanceSchedule(it.first, it.second) }
  }
}
