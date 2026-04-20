package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement.Direction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class CourtEvent(
  val dpsId: UUID?,
  @JsonProperty("prisonCodeAtTimeOfScheduling")
  val scheduledPrisonCode: String,
  @JsonProperty("agyLocId")
  val scheduledCourtCode: String,
  val eventId: Long,
  @JsonProperty("eventDate")
  val date: LocalDate,
  val startTime: LocalTime,
  @JsonProperty("courtEventType")
  val type: String,
  @JsonProperty("eventStatus")
  val status: String,
  val commentText: String?,
) {
  @JsonIgnore
  val start: LocalDateTime = LocalDateTime.of(date, startTime)

  @JsonIgnore
  val end: LocalDateTime = LocalDateTime.of(date, DEFAULT_END_TIME)

  companion object {
    private val DEFAULT_END_TIME = LocalTime.of(17, 0)
  }
}

data class CourtEventMovement(
  val dpsId: UUID?,
  @JsonProperty("dpsCourtAppearanceScheduleId")
  val scheduleId: UUID?,
  @JsonProperty("offenderBookId")
  val bookingId: Long,
  @JsonProperty("movementSeq")
  val sequenceNumber: Int,
  @JsonProperty("movementDate")
  val date: LocalDate,
  @JsonProperty("movementTime")
  val time: LocalTime,
  @JsonProperty("movementReasonCode")
  val reasonCode: String,
  val directionCode: String,
  val fromAgencyId: String,
  val toAgencyId: String,
  val commentText: String?,
) {
  @JsonIgnore
  val legacyId: String = "${bookingId}_$sequenceNumber"

  @JsonIgnore
  val occurredAt: LocalDateTime = LocalDateTime.of(date, time)

  @JsonIgnore
  val direction: Direction = Direction.valueOf(directionCode)

  @JsonIgnore
  val prisonCode: String = when (direction) {
    Direction.OUT -> fromAgencyId
    Direction.IN -> toAgencyId
  }

  @JsonIgnore
  val courtCode: String = when (direction) {
    Direction.OUT -> toAgencyId
    Direction.IN -> fromAgencyId
  }
}
