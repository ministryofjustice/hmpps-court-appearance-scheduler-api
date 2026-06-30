package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceRequestedByVideoLink
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.DomainEvent

class RequestAppearanceByVideoLink : CourtAppearanceAction {
  override fun domainEvent(ca: CourtAppearance): DomainEvent<*> = CourtAppearanceRequestedByVideoLink(ca.person.identifier, ca.id, ca.externalReference)
}
