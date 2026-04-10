package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.PrisonOverview

@Service
class OverviewRetriever(private val appearanceRepository: CourtAppearanceRepository) {
  fun forPrison(prisonCode: String): PrisonOverview = PrisonOverview(
    appearanceRepository.findLeavingToday(prisonCode),
    appearanceRepository.findReturningToday(prisonCode),
  )
}
