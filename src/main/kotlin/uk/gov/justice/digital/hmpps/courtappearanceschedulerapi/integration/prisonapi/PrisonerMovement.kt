package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class PrisonerMovement(
  val toAgency: String?,
  // possible values ["ADM", "CRT", "REL", "TAP", "TRN"]
  val movementType: String?,
  val movementDate: LocalDate,
  val movementTime: LocalTime,
) {
  val movementDateTime: LocalDateTime = LocalDateTime.of(movementDate, movementTime)
  fun isAdmission() = movementType == ADMISSION_TYPE && toAgency != null

  companion object {
    const val ADMISSION_TYPE = "ADM"
  }
}
