package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class ResyncResponse(val courtEvents: List<CourtEventMapping>, val unscheduledMovements: List<CourtMovementMapping>)
data class CourtEventMapping(val dpsId: UUID, val eventId: Long, val movements: List<CourtMovementMapping>)
data class CourtMovementMapping(
  val dpsId: UUID,
  @JsonProperty("offenderBookId") val bookingId: Long,
  @JsonProperty("movementSeq") val sequenceNumber: Int,
)
