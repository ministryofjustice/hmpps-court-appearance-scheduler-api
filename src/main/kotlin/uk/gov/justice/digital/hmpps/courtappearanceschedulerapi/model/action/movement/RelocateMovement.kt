package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.AppearanceMovementRelocated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent

data class RelocateMovement(
  val courtCode: String,
) : AppearanceMovementAction {
  override fun domainEvent(mov: CourtAppearanceMovement): DomainEvent<*> = AppearanceMovementRelocated(mov.person.identifier, mov.id)
}
