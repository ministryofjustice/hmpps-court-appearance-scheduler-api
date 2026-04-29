package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.set
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovementRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReasonProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.StatusProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PersonSummaryService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEventMapping
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtMovementMapping
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ResyncCourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ResyncCourtEventMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ResyncCourtEvents
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ResyncResponse
import java.util.UUID

@Transactional
@Service
class ResyncForPerson(
  private val reasonRepository: CourtAppearanceReasonRepository,
  private val statusRepository: CourtAppearanceStatusRepository,
  private val appearanceRepository: CourtAppearanceRepository,
  private val movementRepository: CourtAppearanceMovementRepository,
  private val msa: MigrationSystemAuditRepository,
  private val personSummaryService: PersonSummaryService,
) {
  fun all(personIdentifier: String, request: ResyncCourtEvents): ResyncResponse {
    SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS, migratingData = true).set()
    val person = personSummaryService.getWithSave(personIdentifier)
    val reasonsMap = reasonRepository.findAll().associateBy { it.code }
    val statusMap = statusRepository.findAll().associateBy { it.code }
    val reason: ReasonProvider = { code -> requireNotNull(reasonsMap[code]) { "Reason code not found" } }
    val status: StatusProvider = { code -> requireNotNull(statusMap[code]) { "Status code not found" } }

    val appearances = with(request.appearanceIds()) {
      findAllAppearances(personIdentifier, first, second)
    }
    val movements = with(request.movementIds()) {
      findUnscheduledMovements(personIdentifier, first, second)
    }
    val appearanceProvider = { id: UUID?, legacyId: Long ->
      appearances.firstOrNull { it.id == id || it.legacyId == legacyId }
    }
    val movementProvider = { id: UUID?, legacyId: String ->
      movements.firstOrNull { it.id == id || it.legacyId == legacyId }
    }

    val scheduled = request.courtEvents.map {
      it.resync(person, appearanceProvider, movementProvider, reason, status)
    }
    val unscheduled = request.unscheduledMovements.map {
      it.resync(person, null, movementProvider, reason, status)
    }

    val (sch, uns) = removeNotInResync(scheduled, unscheduled, appearances, movements)
    if (request.isEmpty() && sch.isEmpty() && uns.isEmpty()) {
      personSummaryService.remove(person)
    }
    return ResyncResponse(scheduled, unscheduled)
  }

  private fun findAllAppearances(
    personIdentifier: String,
    legacyIds: Set<Long>,
    ids: Set<UUID>,
  ): List<CourtAppearance> {
    val forLegacyIds = appearanceRepository.findIdsForLegacyIds(legacyIds)
    val forPi = appearanceRepository.findIdsForPersonIdentifier(personIdentifier)
    return appearanceRepository.findAllById((ids + forLegacyIds + forPi).toSet())
  }

  private fun findUnscheduledMovements(
    personIdentifier: String,
    legacyIds: Set<String>,
    ids: Set<UUID>,
  ): List<CourtAppearanceMovement> {
    val forLegacyIds = movementRepository.findIdsForLegacyIds(legacyIds)
    val forPi = movementRepository.findIdsForPersonIdentifier(personIdentifier)
    return movementRepository.findAllById((ids + forLegacyIds + forPi).toSet())
  }

  private fun ResyncCourtEvent.resync(
    person: PersonSummary,
    appearanceProvider: (UUID?, Long) -> CourtAppearance?,
    movementProvider: (UUID?, String) -> CourtAppearanceMovement?,
    reasonProvider: ReasonProvider,
    statusProvider: StatusProvider,
  ): CourtEventMapping {
    val appearance = appearanceProvider(courtEvent.dpsId, requireNotNull(courtEvent.eventId))
      ?.updateFrom(person, courtEvent, reasonProvider, statusProvider)
      ?: appearanceRepository.save(courtEvent.asEntity(person, reasonProvider, statusProvider))
    val scheduledMovements = movements.map {
      it.resync(person, appearance, movementProvider, reasonProvider, statusProvider)
    }
    mergeMigrationAudit(appearance.id, created, modified)
    return CourtEventMapping(appearance.id, courtEvent.eventId, scheduledMovements)
  }

  private fun ResyncCourtEventMovement.resync(
    person: PersonSummary,
    appearance: CourtAppearance?,
    movementProvider: (UUID?, String) -> CourtAppearanceMovement?,
    reasonProvider: ReasonProvider,
    statusProvider: StatusProvider,
  ): CourtMovementMapping {
    val move = movementProvider(movement.dpsId, requireNotNull(movement.legacyId))
      ?.updateFrom(person, appearance, movement, reasonProvider, statusProvider)
      ?: movementRepository.save(movement.asEntity(person, appearance, reasonProvider, statusProvider))
    mergeMigrationAudit(move.id, created, modified)
    return CourtMovementMapping(move.id, requireNotNull(movement.bookingId), requireNotNull(movement.sequenceNumber))
  }

  private fun removeNotInResync(
    scheduled: List<CourtEventMapping>,
    unscheduled: List<CourtMovementMapping>,
    appearances: List<CourtAppearance>,
    movements: List<CourtAppearanceMovement>,
  ): Pair<List<CourtAppearance>, List<CourtAppearanceMovement>> {
    val movementIds = (scheduled.flatMap { it.movements.map { m -> m.dpsId } } + unscheduled.map { it.dpsId }).toSet()
    val appearancesIds = scheduled.map { it.dpsId }.toSet()
    val (movToKeep, movToDelete) = movements.partition { it.id in movementIds }
    val (appToKeep, appToDelete) = appearances.partition { it.id in appearancesIds }
    movementRepository.deleteAll(movToDelete)
    appearanceRepository.deleteAll(appToDelete)
    return appToKeep to movToKeep
  }

  private fun mergeMigrationAudit(id: UUID, created: AtAndBy, modified: AtAndBy?) {
    msa.save(
      MigrationSystemAudit(id, created.at, created.by, modified?.at, modified?.by),
    )
  }
}
