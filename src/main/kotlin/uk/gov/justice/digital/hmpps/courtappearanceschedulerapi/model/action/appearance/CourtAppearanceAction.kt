package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.Action

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  value = [
    Type(value = ChangeAppearanceComments::class, name = "ChangeAppearanceComments"),
  ],
)
sealed interface CourtAppearanceAction : Action {
  fun domainEvent(ca: CourtAppearance): DomainEvent<*>? = null
}
