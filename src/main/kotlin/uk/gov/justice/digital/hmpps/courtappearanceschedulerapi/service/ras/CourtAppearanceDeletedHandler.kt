package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.ras

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceEntity
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.RasAppearanceDeleted
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ExternalReference
import java.util.UUID

@Transactional
@Service
class CourtAppearanceDeletedHandler(private val appearanceRepository: CourtAppearanceRepository) {
  fun handle(event: RasAppearanceDeleted) {
    appearanceRepository.findByExternalReference(externalReferenceFor(event.additionalInformation.courtAppearanceId))
      ?.also {
        if (it.movements.isEmpty()) {
          appearanceRepository.delete(it)
        } else {
          it.applyExternalIdentifiers(null, it.legacyId)
        }
      }
  }

  private fun externalReferenceFor(uuid: UUID): ExternalReference = ExternalReference(
    ExternalReferenceService.REMAND_AND_SENTENCING,
    ExternalReferenceEntity.COURT_APPEARANCE,
    uuid,
  )
}
