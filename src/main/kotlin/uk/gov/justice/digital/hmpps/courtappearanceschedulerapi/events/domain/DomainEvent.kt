package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import java.time.ZonedDateTime
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "eventType")
@JsonSubTypes(
  value = [
    JsonSubTypes.Type(value = CourtAppearanceMigrated::class, name = CourtAppearanceMigrated.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceScheduled::class, name = CourtAppearanceScheduled.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceUnscheduled::class, name = CourtAppearanceUnscheduled.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceCancelled::class, name = CourtAppearanceCancelled.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceExpired::class, name = CourtAppearanceExpired.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceRecorded::class, name = CourtAppearanceRecorded.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceRecategorised::class, name = CourtAppearanceRecategorised.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceRelocated::class, name = CourtAppearanceRelocated.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceRescheduled::class, name = CourtAppearanceRescheduled.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceCommentsChanged::class, name = CourtAppearanceCommentsChanged.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceResponsiblePrisonChanged::class, name = CourtAppearanceResponsiblePrisonChanged.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceRequestedInPerson::class, name = CourtAppearanceRequestedInPerson.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceRequestedByVideoLink::class, name = CourtAppearanceRequestedByVideoLink.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceStarted::class, name = CourtAppearanceStarted.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceCompleted::class, name = CourtAppearanceCompleted.EVENT_TYPE),

    JsonSubTypes.Type(value = AppearanceMovementMigrated::class, name = AppearanceMovementMigrated.EVENT_TYPE),
    JsonSubTypes.Type(value = AppearanceMovementRecorded::class, name = AppearanceMovementRecorded.EVENT_TYPE),
    JsonSubTypes.Type(value = AppearanceMovementDeleted::class, name = AppearanceMovementDeleted.EVENT_TYPE),
    JsonSubTypes.Type(value = AppearanceMovementRecategorised::class, name = AppearanceMovementRecategorised.EVENT_TYPE),
    JsonSubTypes.Type(value = AppearanceMovementRelocated::class, name = AppearanceMovementRelocated.EVENT_TYPE),
    JsonSubTypes.Type(value = AppearanceMovementCommentsChanged::class, name = AppearanceMovementCommentsChanged.EVENT_TYPE),
    JsonSubTypes.Type(value = AppearanceMovementOccurredAtChanged::class, name = AppearanceMovementOccurredAtChanged.EVENT_TYPE),
    JsonSubTypes.Type(value = AppearanceMovementAppearanceChanged::class, name = AppearanceMovementAppearanceChanged.EVENT_TYPE),
    JsonSubTypes.Type(value = AppearanceMovementReversed::class, name = AppearanceMovementReversed.EVENT_TYPE),

    JsonSubTypes.Type(value = PrisonerMerged::class, name = PrisonerMerged.EVENT_TYPE),
    JsonSubTypes.Type(value = PrisonerUpdated::class, name = PrisonerUpdated.EVENT_TYPE),
    JsonSubTypes.Type(value = PrisonerReceived::class, name = PrisonerReceived.EVENT_TYPE),

    JsonSubTypes.Type(value = RasAppearanceDeleted::class, name = RasAppearanceDeleted.EVENT_TYPE),
  ],
)
sealed interface DomainEvent<T : AdditionalInformation> {
  val occurredAt: ZonedDateTime
    get() = ZonedDateTime.now()

  val eventType: String
  val description: String
  val additionalInformation: T
  val personReference: PersonReference
  val detailUrl: String?
    get() = null
  val version: Int
    get() = 1

  @get:JsonIgnore
  val id: UUID
    get() = newUuid()

  @JsonIgnore
  fun getPersonIdentifier(): String = checkNotNull(personReference.findPersonIdentifier())
}

data class PersonReference(val identifiers: List<Identifier> = listOf()) {
  operator fun get(key: String): String? = identifiers.find { it.type == key }?.value
  fun findPersonIdentifier() = get(NOMS_NUMBER_TYPE)

  companion object {
    const val NOMS_NUMBER_TYPE = "NOMS"
    fun withIdentifier(personIdentifier: String) = PersonReference(listOf(Identifier(NOMS_NUMBER_TYPE, personIdentifier)))
  }

  data class Identifier(val type: String, val value: String)
}

sealed interface AdditionalInformation

sealed interface SourceInformation : AdditionalInformation {
  val source: DataSource
}

sealed interface IdInformation : AdditionalInformation {
  val id: UUID
}
