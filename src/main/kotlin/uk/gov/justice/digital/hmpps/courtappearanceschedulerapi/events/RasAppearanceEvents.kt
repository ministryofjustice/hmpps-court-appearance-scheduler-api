package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events

import java.util.UUID

data class RasAppearanceInformation(val courtAppearanceId: UUID) : AdditionalInformation

data class RasAppearanceDeleted(
  override val additionalInformation: RasAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<RasAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "court-appearance.deleted"
    const val DESCRIPTION: String = "Court appearance deleted"
  }
}
