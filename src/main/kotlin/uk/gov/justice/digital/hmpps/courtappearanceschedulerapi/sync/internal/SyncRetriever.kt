package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovementRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearanceMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEventMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ReconciliationCourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ReconciliationResponse
import java.util.UUID

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
      .map(CourtAppearanceMovement::asMovement)
    return ReconciliationResponse(courtEvents, unscheduled)
  }

  fun courtAppearance(id: UUID): CourtEvent = appearanceRepository.getAppearance(id).asEvent()

  fun courtAppearanceMovement(id: UUID): CourtEventMovement = movementRepository.getMovement(id).asMovement()
}

private fun CourtAppearance.reconcile() = ReconciliationCourtEvent(
  asEvent(),
  movements.map(CourtAppearanceMovement::asMovement),
)

private fun CourtAppearance.asEvent() = CourtEvent(
  id,
  prisonCode,
  courtCode,
  legacyId,
  start,
  reason.code,
  status.code.name,
  comments,
  externalReference,
)

private fun CourtAppearanceMovement.asMovement(): CourtEventMovement {
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
    occurredAt,
    reason.code,
    direction.name,
    from,
    to,
    comments,
  )
}
