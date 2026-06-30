package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceCommentsChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.DomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.CommentsAction

data class ChangeAppearanceComments(
  override val comments: String?,
) : CourtAppearanceAction,
  CommentsAction {
  override fun domainEvent(ca: CourtAppearance): DomainEvent<*> = CourtAppearanceCommentsChanged(ca.person.identifier, ca.id, ca.externalReference)
}
