package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.PrisonerMovement.Companion.DEFAULT_LOCATION
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
  fun responsibilityChangeMovement() = movementType in MOVEMENTS_OF_INTEREST && toAgency != null

  companion object {
    const val ADMISSION_TYPE = "ADM"
    const val RELEASE_TYPE = "REL"
    const val DEFAULT_LOCATION = "OUT"

    private val MOVEMENTS_OF_INTEREST = setOf(ADMISSION_TYPE, RELEASE_TYPE)
  }
}

fun List<PrisonerMovement>.locationAt(at: LocalDateTime): String = movementBefore(at)?.toAgency ?: DEFAULT_LOCATION

fun List<PrisonerMovement>.movementBefore(before: LocalDateTime): PrisonerMovement? {
  val ofInterest = filter { it.responsibilityChangeMovement() }.sortedByDescending { it.movementDateTime }
  return ofInterest.firstOrNull { before.isAfter(it.movementDateTime) }
}

fun List<PrisonerMovement>.mostRecent(): PrisonerMovement = maxBy { it.movementDateTime }
