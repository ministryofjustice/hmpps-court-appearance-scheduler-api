package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.ras

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceEntity
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.RasAppearanceDeleted
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ExternalReference
import java.util.UUID

@Service
class CourtAppearanceDeletedHandler(private val appearanceRepository: CourtAppearanceRepository) {
  fun handle(event: RasAppearanceDeleted) {
    appearanceRepository.findByExternalReference(externalReferenceFor(event.additionalInformation.courtAppearanceId))
      ?.takeIf { it.movements.isEmpty() }
      ?.also(appearanceRepository::delete)
  }

  private fun externalReferenceFor(uuid: UUID): ExternalReference = ExternalReference(
    ExternalReferenceService.REMAND_AND_SENTENCING,
    ExternalReferenceEntity.COURT_APPEARANCE,
    uuid,
  )
}
