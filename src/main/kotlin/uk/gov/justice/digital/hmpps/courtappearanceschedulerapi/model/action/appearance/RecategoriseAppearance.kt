package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceRecategorised
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent

class RecategoriseAppearance(
  val reasonCode: String,
) : CourtAppearanceAction {
  override fun domainEvent(ca: CourtAppearance): DomainEvent<*> = CourtAppearanceRecategorised(ca.person.identifier, ca.id, ca.externalReference)
}
