package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement.Direction.OUT
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceStarted
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent

data class StartAppearance(
  override val reason: String? = null,
) : CourtAppearanceAction {
  override fun domainEvent(ca: CourtAppearance): DomainEvent<*> = CourtAppearanceStarted(
    ca.person.identifier,
    checkNotNull(ca.latestMovement(OUT)).id,
    ca.id,
    ca.externalReference,
  )
}
