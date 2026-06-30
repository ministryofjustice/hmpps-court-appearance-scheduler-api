package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.AppearanceMovementRecategorised
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.DomainEvent

data class RecategoriseMovement(
  val reasonCode: String,
) : AppearanceMovementAction {
  override fun domainEvent(mov: CourtAppearanceMovement): DomainEvent<*> = AppearanceMovementRecategorised(mov.person.identifier, mov.id)
}
