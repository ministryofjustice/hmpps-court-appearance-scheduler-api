package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReasonProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.StatusProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.PrisonerMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.locationAt
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.mostRecent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ChangeAppearanceComments
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ChangeAppearancePrison
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RecategoriseAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RelocateAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RescheduleAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement.ChangeMovementComments
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement.ChangeMovementDirection
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement.ChangeMovementOccurredAt
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement.ChangeMovementSchedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement.RecategoriseMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement.RelocateMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEventMovement
import java.time.LocalDateTime

fun CourtEvent.asEntity(
  person: PersonSummary,
  reason: ReasonProvider,
  status: StatusProvider,
  rasScheduleInfo: CourtAppearanceSchedule?,
  movements: List<PrisonerMovement>,
): CourtAppearance = CourtAppearance(
  person,
  movements.locationAt(start),
  scheduledCourtCode,
  reason(type),
  start,
  end,
  commentText,
  externalReferenceUrn,
  eventId,
  dpsId ?: newUuid(),
).syncStatus(
  status,
  movements.mostRecent()?.movementDateTime?.let { start.isBefore(it) } ?: false,
  currentTerm,
  rasScheduleInfo,
)

fun CourtAppearance.updateFrom(
  personSummary: PersonSummary,
  request: CourtEvent,
  reason: ReasonProvider,
  status: StatusProvider,
  rasScheduleInfo: CourtAppearanceSchedule?,
  movements: List<PrisonerMovement>,
): CourtAppearance = apply {
  movePerson(personSummary)
  applyResponsibility(ChangeAppearancePrison(movements.locationAt(request.start)))
  applyExternalIdentifiers(request.externalReferenceUrn, request.eventId)
  relocate(RelocateAppearance(request.scheduledCourtCode))
  recategorise(RecategoriseAppearance(request.type), reason)
  reschedule(RescheduleAppearance(request.start, request.end))
  applyComments(ChangeAppearanceComments(request.commentText))
  syncStatus(
    status,
    movements.mostRecent()?.movementDateTime?.let { start.isBefore(it) } ?: false,
    request.currentTerm,
    rasScheduleInfo,
  )
}

fun CourtAppearance.syncStatus(
  statusProvider: StatusProvider,
  complete: Boolean,
  currentTerm: Boolean,
  rasScheduleInfo: CourtAppearanceSchedule?,
) = apply {
  val unschedule = rasScheduleInfo?.isDuplicate == true || (!currentTerm && start.isAfter(LocalDateTime.now()))
  calculateStatus(statusProvider, complete, unschedule)
}

fun CourtEventMovement.asEntity(
  person: PersonSummary,
  schedule: CourtAppearance?,
  reason: ReasonProvider,
  statusProvider: StatusProvider,
): CourtAppearanceMovement = CourtAppearanceMovement(
  null,
  person,
  prisonCode,
  courtCode,
  reason(reasonCode),
  direction,
  occurredAt,
  commentText,
  legacyId,
  dpsId ?: newUuid(),
).also {
  schedule?.addMovement(it)?.calculateStatus(statusProvider)
}

fun CourtAppearanceMovement.updateFrom(
  person: PersonSummary,
  schedule: CourtAppearance?,
  request: CourtEventMovement,
  reason: ReasonProvider,
  status: StatusProvider,
) = apply {
  movePerson(person)
  moveSchedule(ChangeMovementSchedule(schedule), status)
  applyDirection(ChangeMovementDirection(request.direction))
  applyOccurredAt(ChangeMovementOccurredAt(request.occurredAt))
  recategorise(RecategoriseMovement(request.reasonCode), reason)
  relocate(RelocateMovement(request.courtCode))
  applyComments(ChangeMovementComments(request.commentText))
  request.legacyId?.also { applyLegacyId(it) }
}
