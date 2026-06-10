package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.AppearanceMovementCommentsChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.CommentsAction

data class ChangeMovementComments(
  override val comments: String?,
  override val reason: String? = null,
) : AppearanceMovementAction,
  CommentsAction {
  override fun domainEvent(mov: CourtAppearanceMovement): DomainEvent<*> = AppearanceMovementCommentsChanged(mov.person.identifier, mov.id)
}
