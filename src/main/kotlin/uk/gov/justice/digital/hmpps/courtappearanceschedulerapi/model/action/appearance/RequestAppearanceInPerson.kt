package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRequestedInPerson
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent

class RequestAppearanceInPerson : CourtAppearanceAction {
  override fun domainEvent(ca: CourtAppearance): DomainEvent<*> = CourtAppearanceRequestedInPerson(ca.person.identifier, ca.id, ca.externalReference)
}
