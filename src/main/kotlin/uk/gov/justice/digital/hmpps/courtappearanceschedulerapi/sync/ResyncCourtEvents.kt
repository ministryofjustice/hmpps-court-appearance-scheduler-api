package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime
import java.util.UUID

data class ResyncCourtEvents(
  val courtEvents: List<ResyncCourtEvent>,
  val unscheduledMovements: List<ResyncCourtEventMovement>,
) {
  @JsonIgnore
  fun isEmpty(): Boolean = courtEvents.isEmpty() && unscheduledMovements.isEmpty()

  fun appearanceIds(): Pair<Set<Long>, Set<UUID>> {
    val (legacyIds, ids) = courtEvents.map { re -> requireNotNull(re.courtEvent.eventId) to re.courtEvent.dpsId }.unzip()
    return legacyIds.toSet() to ids.filterNotNull().toSet()
  }

  fun movementIds(): Pair<Set<String>, Set<UUID>> {
    val (legacyIds, ids) = (
      unscheduledMovements.map { requireNotNull(it.movement.legacyId) to it.movement.dpsId } +
        courtEvents.flatMap { it.movements.map { e -> requireNotNull(e.movement.legacyId) to e.movement.dpsId } }
      ).unzip()
    return legacyIds.toSet() to ids.filterNotNull().toSet()
  }

  fun includesRas(): Boolean = courtEvents.any { it.courtEvent.externalReferenceUrn?.uuid != null }
}

data class ResyncCourtEvent(
  val courtEvent: CourtEvent,
  val created: AtAndBy,
  val modified: AtAndBy?,
  val movements: List<ResyncCourtEventMovement>,
)

data class ResyncCourtEventMovement(
  val movement: CourtEventMovement,
  val created: AtAndBy,
  val modified: AtAndBy?,
)

data class AtAndBy(val at: LocalDateTime, val by: String)
