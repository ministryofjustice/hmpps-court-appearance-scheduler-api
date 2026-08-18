package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceReason
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.CourtAppearanceReasons
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.asReason

@Service
class ReasonRetriever(private val reasonRepository: CourtAppearanceReasonRepository) {
  fun allReasons(): CourtAppearanceReasons = CourtAppearanceReasons(
    reasonRepository.findAll().filter { it.active }.sortedBy { it.sequenceNumber }.map(CourtAppearanceReason::asReason),
  )
}
