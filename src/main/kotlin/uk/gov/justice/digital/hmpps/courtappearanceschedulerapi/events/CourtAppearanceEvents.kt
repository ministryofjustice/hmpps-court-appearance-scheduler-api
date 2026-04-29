package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import java.util.UUID

data class AdditionalAppearanceInformation(
  override val id: UUID,
  override val source: DataSource,
  val externalReferenceUrn: String?,
) : SourceInformation,
  IdInformation

data class CourtAppearanceMigrated(
  override val additionalInformation: AdditionalAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE = "person.court-appearance.migrated"
    const val DESCRIPTION = "A court appearance schedule has been migrated"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceMigrated(
      AdditionalAppearanceInformation(id, dataSource, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class CourtAppearanceScheduled(
  override val additionalInformation: AdditionalAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE = "person.court-appearance.scheduled"
    const val DESCRIPTION = "A court appearance has been scheduled"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceScheduled(
      AdditionalAppearanceInformation(id, dataSource, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class CourtAppearanceExpired(
  override val additionalInformation: AdditionalAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE = "person.court-appearance.expired"
    const val DESCRIPTION = "A court appearance schedule has been expired"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceExpired(
      AdditionalAppearanceInformation(id, dataSource, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class CourtAppearanceRecorded(
  override val additionalInformation: AdditionalAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE = "person.court-appearance.recorded"
    const val DESCRIPTION = "A court appearance has been recorded"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceRecorded(
      AdditionalAppearanceInformation(id, dataSource, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class CourtAppearanceRecategorised(
  override val additionalInformation: AdditionalAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE = "person.court-appearance.recategorised"
    const val DESCRIPTION = "The reason for a court appearance has changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceRecategorised(
      AdditionalAppearanceInformation(id, dataSource, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class CourtAppearanceRelocated(
  override val additionalInformation: AdditionalAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE = "person.court-appearance.relocated"
    const val DESCRIPTION = "The court has changed for a court appearance"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceRelocated(
      AdditionalAppearanceInformation(id, dataSource, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class CourtAppearanceRescheduled(
  override val additionalInformation: AdditionalAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE = "person.court-appearance.rescheduled"
    const val DESCRIPTION = "A court appearance has been rescheduled"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceRescheduled(
      AdditionalAppearanceInformation(id, dataSource, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class CourtAppearanceCommentsChanged(
  override val additionalInformation: AdditionalAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE = "person.court-appearance.comments-changed"
    const val DESCRIPTION = "The comments on a court appearance have changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceCommentsChanged(
      AdditionalAppearanceInformation(id, dataSource, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class CourtAppearanceRequestedInPerson(
  override val additionalInformation: AdditionalAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE = "person.court-appearance.requested-in-person"
    const val DESCRIPTION = "A court appearance has been requested to be in-person"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceRequestedInPerson(
      AdditionalAppearanceInformation(id, dataSource, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class CourtAppearanceRequestedByVideoLink(
  override val additionalInformation: AdditionalAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE = "person.court-appearance.requested-by-video-link"
    const val DESCRIPTION = "A court appearance has been requested by video link"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceRequestedByVideoLink(
      AdditionalAppearanceInformation(id, dataSource, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class CourtAppearanceStarted(
  override val additionalInformation: AdditionalAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE = "person.court-appearance.started"
    const val DESCRIPTION = "A court appearance has been started"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceStarted(
      AdditionalAppearanceInformation(id, dataSource, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class CourtAppearanceCompleted(
  override val additionalInformation: AdditionalAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE = "person.court-appearance.completed"
    const val DESCRIPTION = "A court appearance has been completed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceCompleted(
      AdditionalAppearanceInformation(id, dataSource, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
