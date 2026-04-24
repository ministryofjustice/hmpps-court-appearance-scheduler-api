package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model

import java.time.LocalDateTime

data class ScheduleCourtAppearance(
  val courtCode: String,
  val reasonCode: String,
  val start: LocalDateTime,
  val end: LocalDateTime?,
  val comments: String?,
)
