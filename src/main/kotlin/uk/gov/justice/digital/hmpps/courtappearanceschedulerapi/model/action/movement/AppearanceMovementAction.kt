package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement

import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.DomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.Action

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface AppearanceMovementAction : Action {
  fun domainEvent(mov: CourtAppearanceMovement): DomainEvent<*>? = null
}
