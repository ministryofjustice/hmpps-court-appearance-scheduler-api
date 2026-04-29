package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync

data class ReconciliationResponse(
  val courtEvents: List<ReconciliationCourtEvent>,
  val unscheduledMovements: List<CourtEventMovement>,
)

data class ReconciliationCourtEvent(
  val courtEvent: CourtEvent,
  val movements: List<CourtEventMovement>,
)
