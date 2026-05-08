package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationUrlBuilder.movementUrl
import java.util.UUID

data class ProgressInformation(
  override val id: UUID,
  override val source: DataSource,
  val scheduleId: UUID?,
  val externalReferenceUrn: String?,
) : SourceInformation,
  IdInformation

data class CourtAppearanceStarted(
  override val additionalInformation: ProgressInformation,
  override val personReference: PersonReference,
) : DomainEvent<ProgressInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(id)

  companion object {
    const val EVENT_TYPE = "person.court-appearance.started"
    const val DESCRIPTION = "A court appearance has been started"
    operator fun invoke(
      personIdentifier: String,
      movementId: UUID,
      scheduleId: UUID?,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceStarted(
      ProgressInformation(movementId, dataSource, scheduleId, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class CourtAppearanceCompleted(
  override val additionalInformation: ProgressInformation,
  override val personReference: PersonReference,
) : DomainEvent<ProgressInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = movementUrl(id)

  companion object {
    const val EVENT_TYPE = "person.court-appearance.completed"
    const val DESCRIPTION = "A court appearance has been completed"
    operator fun invoke(
      personIdentifier: String,
      movementId: UUID,
      scheduleId: UUID?,
      externalReference: String?,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = CourtAppearanceCompleted(
      ProgressInformation(movementId, dataSource, scheduleId, externalReference),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
