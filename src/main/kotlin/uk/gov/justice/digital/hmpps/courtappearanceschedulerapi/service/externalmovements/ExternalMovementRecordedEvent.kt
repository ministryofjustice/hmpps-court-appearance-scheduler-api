package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.externalmovements

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ExternalMovementRecordedEvent(
  @JsonProperty("offenderIdDisplay")
  val personIdentifier: String? = null,
  val movementType: String? = null,
  val directionCode: String? = null,
  @JsonProperty("toAgencyLocationId")
  val prisonCode: String? = null,
  val movementDateTime: LocalDateTime? = null,
) {
  companion object {
    const val EVENT_TYPE = "EXTERNAL_MOVEMENT_RECORD-INSERTED"
  }
}
