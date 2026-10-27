package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearance.Companion.DEFAULT_END_TIME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceMovement.Direction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.externalreference.ExternalReference
import java.time.LocalDateTime
import java.util.UUID

data class CourtEvent(
  val dpsId: UUID?,
  @JsonProperty("agyLocId")
  val scheduledCourtCode: String,
  val eventId: Long?,
  val start: LocalDateTime,
  @JsonProperty("courtEventType")
  val type: String,
  @JsonProperty("eventStatus")
  val status: String,
  val commentText: String?,
  @Schema(type = "string", nullable = true, requiredMode = NOT_REQUIRED)
  val externalReferenceUrn: ExternalReference?,
  val externalCourtEventType: Boolean?,
  val currentTerm: Boolean = true,
) {
  @JsonIgnore
  val end: LocalDateTime = LocalDateTime.of(start.toLocalDate(), maxOf(DEFAULT_END_TIME, start.toLocalTime()))
}

data class CourtEventMovement(
  val dpsId: UUID?,
  @JsonProperty("dpsCourtAppearanceScheduleId")
  val scheduleId: UUID?,
  @JsonProperty("dpsCourtAppearanceExternalReference")
  val scheduleExternalReference: ExternalReference?,
  @JsonProperty("offenderBookId")
  val bookingId: Long?,
  @JsonProperty("movementSeq")
  val sequenceNumber: Int?,
  val occurredAt: LocalDateTime,
  @JsonProperty("movementReasonCode")
  val reasonCode: String,
  val directionCode: String,
  val fromAgencyId: String,
  val toAgencyId: String,
  val commentText: String?,
) {
  @JsonIgnore
  val legacyId: String? = if (bookingId != null && sequenceNumber != null) "${bookingId}_$sequenceNumber" else null

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
