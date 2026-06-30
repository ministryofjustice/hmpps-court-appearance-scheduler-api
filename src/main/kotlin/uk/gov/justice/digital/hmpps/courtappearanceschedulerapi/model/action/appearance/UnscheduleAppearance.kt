package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceUnscheduled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.DomainEvent

class UnscheduleAppearance : CourtAppearanceAction {
  override fun domainEvent(ca: CourtAppearance): DomainEvent<*> = CourtAppearanceUnscheduled(ca.person.identifier, ca.id, ca.externalReference)
}
