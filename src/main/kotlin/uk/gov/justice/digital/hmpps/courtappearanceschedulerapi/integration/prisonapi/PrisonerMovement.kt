package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi

import java.time.LocalDateTime

data class PrisonerMovement(
  val toAgency: String?,
  // possible values ["ADM", "CRT", "REL", "TAP", "TRN"]
  val movementType: String?,
  val movementDateTime: LocalDateTime?,
) {
  fun isAdmission() = movementType == ADMISSION_TYPE && toAgency != null && movementDateTime != null

  companion object {
    const val ADMISSION_TYPE = "ADM"
  }
}
