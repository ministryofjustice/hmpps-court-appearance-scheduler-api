package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.AppearanceMovementOccurredAtChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

data class ChangeMovementOccurredAt(
  val occurredAt: LocalDateTime,
  override val reason: String? = null,
) : AppearanceMovementAction {
  override fun domainEvent(mov: CourtAppearanceMovement): DomainEvent<*> = AppearanceMovementOccurredAtChanged(mov.person.identifier, mov.id)

  infix fun changes(other: LocalDateTime): Boolean = !occurredAt.truncatedTo(SECONDS).isEqual(other.truncatedTo(SECONDS))
}
