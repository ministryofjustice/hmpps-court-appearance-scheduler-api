package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class CourtAppearanceClonedInformation(
  val nomsNumber: String,
  @JsonProperty("previousBookingAppearanceId")
  val previousId: UUID,
  @JsonProperty("currentBookingAppearanceId")
  val clonedId: UUID,
) : AdditionalInformation

data class CourtAppearanceCloned(
  override val additionalInformation: CourtAppearanceClonedInformation,
  override val personReference: PersonReference,
) : DomainEvent<CourtAppearanceClonedInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "nomis-sync.court-appearance.cloned"
    const val DESCRIPTION: String = "A court appearance has been cloned in nomis"
  }
}
