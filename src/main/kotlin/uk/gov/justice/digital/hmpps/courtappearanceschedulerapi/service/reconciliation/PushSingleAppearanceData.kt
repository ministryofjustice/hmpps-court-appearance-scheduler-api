package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.RemandAndSentencingClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.asUpdateRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ExternalReference

@Service
class PushSingleAppearanceData(
  private val caRepository: CourtAppearanceRepository,
  private val rasClient: RemandAndSentencingClient,
) {
  fun toExternalService(externalReference: ExternalReference) {
    if (externalReference.service != ExternalReferenceService.REMAND_AND_SENTENCING) return
    caRepository.findByExternalReference(externalReference)?.let { ca ->
      rasClient.updateCourtAppearanceSchedule(externalReference.uuid, ca.asUpdateRequest())
    }
  }
}
