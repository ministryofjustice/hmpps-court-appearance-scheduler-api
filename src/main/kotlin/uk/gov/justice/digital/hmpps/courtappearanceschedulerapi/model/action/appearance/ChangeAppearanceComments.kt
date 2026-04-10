package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceCommentsChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent
import kotlin.reflect.KMutableProperty0

data class ChangeAppearanceComments(
  val comments: String?,
  override val reason: String? = null,
) : CourtAppearanceAction {
  override fun domainEvent(ca: CourtAppearance): DomainEvent<*> = CourtAppearanceCommentsChanged(ca.person.identifier, ca.id)
}

infix fun ChangeAppearanceComments.changes(property: KMutableProperty0<String?>): Boolean = if (comments == property.get()) {
  false
} else {
  property.set(comments)
  true
}
