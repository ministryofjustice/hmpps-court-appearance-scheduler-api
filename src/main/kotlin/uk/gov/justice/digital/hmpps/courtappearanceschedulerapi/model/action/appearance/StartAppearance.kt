package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceStarted
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.DomainEvent

object StartAppearance : CourtAppearanceAction {
  override fun domainEvent(ca: CourtAppearance): DomainEvent<*> = CourtAppearanceStarted(ca.person.identifier, ca.id, ca.externalReference)
}
