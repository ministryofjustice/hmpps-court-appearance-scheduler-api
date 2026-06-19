package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.externalmovements

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearanceMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearanceStatusCodeIn
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ChangeAppearancePrison

@Service
class ExternalMovementHandler(private val caRepository: CourtAppearanceRepository) {
  @Transactional
  fun handle(emre: ExternalMovementRecordedEvent) {
    if (emre.directionCode != "IN" || emre.movementType != "ADM" || emre.personIdentifier == null || emre.prisonCode == null) return
    caRepository.findAll(
      appearanceMatchesPersonIdentifier(emre.personIdentifier, null)
        .and(appearanceStatusCodeIn(setOf(CourtAppearanceStatus.Code.SCHEDULED))),
    ).forEach { it.applyResponsibility(ChangeAppearancePrison(emre.prisonCode)) }
  }
}
