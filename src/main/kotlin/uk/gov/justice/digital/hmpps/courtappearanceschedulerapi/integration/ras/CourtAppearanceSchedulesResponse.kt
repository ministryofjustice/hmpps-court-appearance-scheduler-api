package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

data class CourtAppearanceSchedulesResponse(val courtAppearances: List<CourtAppearanceSchedule>)
data class CourtAppearanceSchedule(
  val id: UUID,
  val personIdentifier: String,
  val courtCode: String,
  val reason: ScheduleReason,
  val start: LocalDateTime,
  @JsonProperty("isDuplicate")
  val isDuplicate: Boolean,
) {
  data class ScheduleReason(val code: String)
}
