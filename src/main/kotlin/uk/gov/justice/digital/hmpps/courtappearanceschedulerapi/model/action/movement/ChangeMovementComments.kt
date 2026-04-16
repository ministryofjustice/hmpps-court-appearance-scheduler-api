package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.AppearanceMovementCommentsChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent
import kotlin.reflect.KMutableProperty0

data class ChangeMovementComments(
  val comments: String?,
  override val reason: String? = null,
) : AppearanceMovementAction {
  override fun domainEvent(mov: CourtAppearanceMovement): DomainEvent<*> = AppearanceMovementCommentsChanged(mov.person.identifier, mov.id)
}

infix fun ChangeMovementComments.changes(property: KMutableProperty0<String?>): Boolean = if (comments == property.get()) {
  false
} else {
  property.set(comments)
  true
}
