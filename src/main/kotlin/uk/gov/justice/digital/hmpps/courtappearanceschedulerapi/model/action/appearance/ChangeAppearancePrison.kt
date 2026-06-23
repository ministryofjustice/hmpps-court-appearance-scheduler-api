package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceResponsiblePrisonChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent

data class ChangeAppearancePrison(
  val prisonCode: String,
) : CourtAppearanceAction {
  override fun domainEvent(ca: CourtAppearance): DomainEvent<*> = CourtAppearanceResponsiblePrisonChanged(ca.person.identifier, ca.id, ca.externalReference)
}
