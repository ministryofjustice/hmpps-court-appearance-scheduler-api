package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.AppearanceMovementAppearanceChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent

data class ChangeMovementSchedule(
  val appearance: CourtAppearance?,
  override val reason: String? = null,
) : AppearanceMovementAction {
  override fun domainEvent(mov: CourtAppearanceMovement): DomainEvent<*> = AppearanceMovementAppearanceChanged(mov.person.identifier, mov.id)
}
