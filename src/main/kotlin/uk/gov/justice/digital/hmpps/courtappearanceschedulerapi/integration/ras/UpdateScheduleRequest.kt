package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras

import java.time.LocalDateTime

data class UpdateScheduleRequest(
  val prisonCode: String,
  val courtCode: String,
  val reasonCode: String,
  val start: LocalDateTime,
  val comments: String?,
)
