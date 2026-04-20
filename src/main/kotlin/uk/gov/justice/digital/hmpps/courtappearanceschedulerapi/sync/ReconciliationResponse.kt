package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync

data class ReconciliationResponse(
  val courtEvents: List<CourtEvent>,
  val unscheduledMovements: List<CourtEventMovement>,
)
