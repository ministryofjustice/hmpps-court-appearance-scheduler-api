package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceEntity
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ExternalReference
import java.util.UUID

data class RasAppearanceInformation(val courtAppearanceId: UUID) : AdditionalInformation {
  fun externalReference(): ExternalReference = ExternalReference(
    ExternalReferenceService.REMAND_AND_SENTENCING,
    ExternalReferenceEntity.COURT_APPEARANCE,
    courtAppearanceId,
  )
}

interface RasAppearanceEvent : DomainEvent<RasAppearanceInformation> {
  fun externalReference(): ExternalReference = additionalInformation.externalReference()
}

data class RasAppearanceDeleted(
  override val additionalInformation: RasAppearanceInformation,
  override val personReference: PersonReference,
) : RasAppearanceEvent {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "court-appearance.deleted"
    const val DESCRIPTION: String = "Court appearance deleted"
  }
}

data class RasAppearanceInserted(
  override val additionalInformation: RasAppearanceInformation,
  override val personReference: PersonReference,
) : RasAppearanceEvent {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "court-appearance.inserted"
    const val DESCRIPTION: String = "Court appearance inserted"
  }
}

data class RasAppearanceUpdated(
  override val additionalInformation: RasAppearanceInformation,
  override val personReference: PersonReference,
) : RasAppearanceEvent {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "court-appearance.updated"
    const val DESCRIPTION: String = "Court appearance updated"
  }
}
