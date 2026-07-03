package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.RemandAndSentencingClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.UpdateScheduleRequest
import java.time.temporal.ChronoUnit

@Service
class PushPersonAppearanceData(
  private val caRepository: CourtAppearanceRepository,
  private val rasClient: RemandAndSentencingClient,
) {
  fun toRemandAndSentencing(identifier: String) {
    val rasAppearances = rasClient.findScheduleAppearancesFor(identifier).courtAppearances.associateBy { it.id }
    val appearances = caRepository.findByPersonIdentifierAndExternalReferenceIsNotNull(identifier)
      .filter { it.externalReference?.service == ExternalReferenceService.REMAND_AND_SENTENCING && it diff rasAppearances[it.externalReference!!.uuid] }

    Flux.fromIterable(appearances)
      .flatMap(
        { rasClient.updateCourtAppearanceSchedule(it.externalReference!!.uuid, it.asUpdateRequest()) },
        2,
      ).blockLast()
  }

  private infix fun CourtAppearance.diff(schedule: CourtAppearanceSchedule?): Boolean = schedule?.let {
    it.comments != comments ||
      it.reason.code != reason.code ||
      !it.start.truncatedTo(ChronoUnit.SECONDS).isEqual(start.truncatedTo(ChronoUnit.SECONDS))
  } ?: false

  private fun CourtAppearance.asUpdateRequest() = UpdateScheduleRequest(
    prisonCode,
    courtCode,
    reason.code,
    start,
    comments,
  )
}
