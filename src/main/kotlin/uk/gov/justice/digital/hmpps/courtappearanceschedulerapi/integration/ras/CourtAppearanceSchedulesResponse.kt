package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceEntity.COURT_APPEARANCE
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService.REMAND_AND_SENTENCING
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ExternalReference
import java.time.LocalDateTime
import java.time.LocalTime
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
  val comments: String?,
) {
  data class ScheduleReason(val code: String)

  @JsonIgnore
  val end: LocalDateTime = LocalDateTime.of(start.toLocalDate(), maxOf(DEFAULT_END_TIME, start.toLocalTime()))

  @JsonIgnore
  val externalReference: ExternalReference = ExternalReference(REMAND_AND_SENTENCING, COURT_APPEARANCE, id)

  companion object {
    private val DEFAULT_END_TIME = LocalTime.of(17, 0)
  }
}
