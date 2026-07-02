package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.RemandAndSentencingClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.UpdateScheduleRequest

@Service
class PushPersonAppearanceData(
  private val caRepository: CourtAppearanceRepository,
  private val rasClient: RemandAndSentencingClient,
) {
  fun toRemandAndSentencing(identifier: String) {
    val appearances = caRepository.findByPersonIdentifierAndExternalReferenceIsNotNull(identifier)
    Flux.fromIterable(appearances).flatMap(
      { rasClient.updateCourtAppearanceSchedule(it.externalReference!!.uuid, it.asUpdateRequest()) },
      10,
    ).collectList().block()
  }

  private fun CourtAppearance.asUpdateRequest() = UpdateScheduleRequest(
    prisonCode,
    courtCode,
    reason.code,
    start,
    comments,
  )
}
