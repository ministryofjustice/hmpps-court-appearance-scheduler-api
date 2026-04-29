package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovementRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearanceMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEventMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ReconciliationCourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ReconciliationResponse

@Transactional(readOnly = true)
@Service
class SyncRetriever(
  private val appearanceRepository: CourtAppearanceRepository,
  private val movementRepository: CourtAppearanceMovementRepository,
) {
  fun allFor(personIdentifier: String): ReconciliationResponse {
    val courtEvents = appearanceRepository.findAll(appearanceMatchesPersonIdentifier(personIdentifier, null))
      .map(CourtAppearance::reconcile)
    val unscheduled = movementRepository.findAllUnscheduledByPersonIdentifier(personIdentifier)
      .map(CourtAppearanceMovement::reconcile)
    return ReconciliationResponse(courtEvents, unscheduled)
  }
}

private fun CourtAppearance.reconcile() = ReconciliationCourtEvent(
  CourtEvent(
    id,
    prisonCode,
    courtCode,
    legacyId,
    start.toLocalDate(),
    start.toLocalTime(),
    reason.code,
    status.code.name,
    comments,
    externalReference,
  ),
  movements.map(CourtAppearanceMovement::reconcile),
)

private fun CourtAppearanceMovement.reconcile(): CourtEventMovement {
  val legacyIdParts = legacyId?.split("_")
  val (from, to) = when (direction) {
    CourtAppearanceMovement.Direction.OUT -> prisonCode to courtCode
    CourtAppearanceMovement.Direction.IN -> courtCode to prisonCode
  }
  return CourtEventMovement(
    id,
    courtAppearance?.id,
    legacyIdParts?.get(0)?.toLong(),
    legacyIdParts?.get(1)?.toInt(),
    occurredAt.toLocalDate(),
    occurredAt.toLocalTime(),
    reason.code,
    direction.name,
    from,
    to,
    comments,
  )
}
