package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceExpired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.DomainEvent

class ExpireAppearance : CourtAppearanceAction {
  override fun domainEvent(ca: CourtAppearance): DomainEvent<*> = CourtAppearanceExpired(ca.person.identifier, ca.id, ca.externalReference)
}
