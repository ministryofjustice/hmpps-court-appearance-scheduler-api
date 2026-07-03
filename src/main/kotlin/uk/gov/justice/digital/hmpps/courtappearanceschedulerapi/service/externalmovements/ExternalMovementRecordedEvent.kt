package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.externalmovements

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.PrisonerMovement
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
  fun isAdmission() = directionCode == "IN" && movementType == PrisonerMovement.ADMISSION_TYPE
  fun isRelease() = directionCode == "OUT" && movementType == PrisonerMovement.RELEASE_TYPE

  fun isReleaseOrAdmission() = isAdmission() || isRelease()

  companion object {
    const val EVENT_TYPE = "EXTERNAL_MOVEMENT_RECORD-INSERTED"
  }
}
