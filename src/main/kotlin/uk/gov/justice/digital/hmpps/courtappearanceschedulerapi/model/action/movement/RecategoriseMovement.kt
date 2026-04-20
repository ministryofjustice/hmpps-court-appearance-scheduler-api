package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.AppearanceMovementRecategorised
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent

data class RecategoriseMovement(
  val reasonCode: String,
  override val reason: String? = null,
) : AppearanceMovementAction {
  override fun domainEvent(mov: CourtAppearanceMovement): DomainEvent<*> = AppearanceMovementRecategorised(mov.person.identifier, mov.id)
}
