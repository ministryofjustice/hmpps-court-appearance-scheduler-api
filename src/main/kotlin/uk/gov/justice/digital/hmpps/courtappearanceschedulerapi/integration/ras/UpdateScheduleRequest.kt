package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearance
import java.time.LocalDateTime

data class UpdateScheduleRequest(
  val courtCode: String,
  val reasonCode: String,
  val start: LocalDateTime,
  val comments: String?,
)

fun CourtAppearance.asUpdateRequest() = UpdateScheduleRequest(courtCode, reason.code, start, comments)
