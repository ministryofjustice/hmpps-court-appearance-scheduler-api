package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReasonProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.StatusProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ChangeAppearanceComments
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

fun CourtEvent.asEntity(
  person: PersonSummary,
  reason: ReasonProvider,
  status: StatusProvider,
): CourtAppearance = CourtAppearance(
  person,
  scheduledPrisonCode,
  scheduledCourtCode,
  reason(type),
  start,
  end,
  commentText,
  externalReferenceUrn,
  eventId,
  dpsId ?: newUuid(),
).calculateStatus(status).also {
  if (shouldBeCompleted()) {
    it.complete(status)
  }
}

fun CourtAppearance.updateFrom(
  personSummary: PersonSummary,
  request: CourtEvent,
  reason: ReasonProvider,
  status: StatusProvider,
): CourtAppearance = apply {
  movePerson(personSummary)
  applyExternalIdentifiers(request.externalReferenceUrn, request.eventId)
  relocate(RelocateAppearance(request.scheduledCourtCode))
  recategorise(RecategoriseAppearance(request.type), reason)
  reschedule(RescheduleAppearance(request.start, null))
  applyComments(ChangeAppearanceComments(request.commentText))
  calculateStatus(status)
  if (request.shouldBeCompleted()) {
    complete(status)
  }
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
  applyDirection(ChangeMovementDirection(direction))
  applyOccurredAt(ChangeMovementOccurredAt(request.occurredAt))
  recategorise(RecategoriseMovement(request.reasonCode), reason)
  relocate(RelocateMovement(request.courtCode))
  applyComments(ChangeMovementComments(request.commentText))
  request.legacyId?.also { applyLegacyId(it) }
}
