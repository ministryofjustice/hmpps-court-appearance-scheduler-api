package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationUrlBuilder.movementUrl
import java.util.UUID

data class AdditionalMovementInformation(
  override val id: UUID,
  override val source: DataSource,
) : SourceInformation,
  IdInformation

data class AppearanceMovementMigrated(
  override val additionalInformation: AdditionalMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(id)

  companion object {
    const val EVENT_TYPE = "person.court-appearance-movement.migrated"
    const val DESCRIPTION = "A court appearance movement has been migrated"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = AppearanceMovementMigrated(
      AdditionalMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class AppearanceMovementRecorded(
  override val additionalInformation: AdditionalMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(id)

  companion object {
    const val EVENT_TYPE = "person.court-appearance-movement.recorded"
    const val DESCRIPTION = "A court appearance movement has been recorded"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = AppearanceMovementRecorded(
      AdditionalMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class AppearanceMovementDeleted(
  override val additionalInformation: AdditionalMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(id)

  companion object {
    const val EVENT_TYPE = "person.court-appearance-movement.deleted"
    const val DESCRIPTION = "A court appearance movement has been deleted"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = AppearanceMovementDeleted(
      AdditionalMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class AppearanceMovementRecategorised(
  override val additionalInformation: AdditionalMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(id)

  companion object {
    const val EVENT_TYPE = "person.court-appearance-movement.recategorised"
    const val DESCRIPTION = "A court appearance movement has been recategorised"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = AppearanceMovementRecategorised(
      AdditionalMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class AppearanceMovementRelocated(
  override val additionalInformation: AdditionalMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(id)

  companion object {
    const val EVENT_TYPE = "person.court-appearance-movement.relocated"
    const val DESCRIPTION = "The court has changed for a court appearance movement"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = AppearanceMovementRelocated(
      AdditionalMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class AppearanceMovementCommentsChanged(
  override val additionalInformation: AdditionalMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(id)

  companion object {
    const val EVENT_TYPE = "person.court-appearance-movement.comments-changed"
    const val DESCRIPTION = "The comments on a court appearance movement have changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = AppearanceMovementCommentsChanged(
      AdditionalMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class AppearanceMovementOccurredAtChanged(
  override val additionalInformation: AdditionalMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(id)

  companion object {
    const val EVENT_TYPE = "person.court-appearance-movement.occurred-at-changed"
    const val DESCRIPTION = "When a court movement occurred has been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = AppearanceMovementOccurredAtChanged(
      AdditionalMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class AppearanceMovementAppearanceChanged(
  override val additionalInformation: AdditionalMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(id)

  companion object {
    const val EVENT_TYPE = "person.court-appearance-movement.appearance-changed"
    const val DESCRIPTION = "The appearance for a movement has changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = AppearanceMovementAppearanceChanged(
      AdditionalMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class AppearanceMovementReversed(
  override val additionalInformation: AdditionalMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<AdditionalMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(id)

  companion object {
    const val EVENT_TYPE = "person.court-appearance-movement.reversed"
    const val DESCRIPTION = "The court movement direction has been reversed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = AppearanceMovementReversed(
      AdditionalMovementInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
