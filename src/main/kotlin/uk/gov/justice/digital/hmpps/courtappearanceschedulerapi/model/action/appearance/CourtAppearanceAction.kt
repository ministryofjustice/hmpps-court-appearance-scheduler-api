package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.DomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.Action

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface CourtAppearanceAction : Action {
  fun domainEvent(ca: CourtAppearance): DomainEvent<*>? = null
}

data class CourtAppearanceActions(@Valid val actions: List<CourtAppearanceAction>, val reason: String?)
