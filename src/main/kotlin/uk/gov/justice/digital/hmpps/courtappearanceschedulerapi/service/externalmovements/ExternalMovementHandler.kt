package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.externalmovements

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.appearanceMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.getStatusByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.startsAtOrAfter

@Service
class ExternalMovementHandler(
  private val caRepository: CourtAppearanceRepository,
  private val statusRepository: CourtAppearanceStatusRepository,
) {
  @Transactional
  fun handle(emre: ExternalMovementRecordedEvent) {
    if (!emre.isReleaseOrAdmission() || emre.personIdentifier == null || emre.prisonCode == null || emre.movementDateTime == null) return
    caRepository.findAll(
      appearanceMatchesPersonIdentifier(emre.personIdentifier, null)
        .and(startsAtOrAfter(emre.movementDateTime)),
    ).forEach {
      if (it.status.code == CourtAppearanceStatus.Code.IN_PROGRESS) {
        it.calculateStatus(statusRepository::getStatusByCode, completeOverride = true)
      }
    }
  }
}
