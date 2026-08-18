package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceCancelled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.DomainEvent

object CancelAppearance : CourtAppearanceAction {
  override fun domainEvent(ca: CourtAppearance): DomainEvent<*> = CourtAppearanceCancelled(ca.person.identifier, ca.id, ca.externalReference)
}
