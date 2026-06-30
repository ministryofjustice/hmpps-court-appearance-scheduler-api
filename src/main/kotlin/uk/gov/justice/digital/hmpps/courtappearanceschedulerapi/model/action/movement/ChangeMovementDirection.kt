package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.AppearanceMovementReversed
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.DomainEvent

data class ChangeMovementDirection(
  val direction: CourtAppearanceMovement.Direction,
) : AppearanceMovementAction {
  override fun domainEvent(mov: CourtAppearanceMovement): DomainEvent<*> = AppearanceMovementReversed(mov.person.identifier, mov.id)
}
