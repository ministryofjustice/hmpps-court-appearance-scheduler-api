package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
  value = [
    JsonSubTypes.Type(value = CourtAppearanceReconcilePrison::class, name = CourtAppearanceReconcilePrison.EVENT_TYPE),
    JsonSubTypes.Type(value = CourtAppearanceReconcilePerson::class, name = CourtAppearanceReconcilePerson.EVENT_TYPE),
  ],
)
sealed interface InternalEvent {
  val occurredAt: LocalDateTime
  val type: String
}

data class CourtAppearanceReconcilePrison(val prisonCode: String) : InternalEvent {
  override val occurredAt: LocalDateTime = LocalDateTime.now()
  override val type: String = EVENT_TYPE

  companion object {
    const val EVENT_TYPE = "court-appearance.reconcile.prison"
  }
}

data class CourtAppearanceReconcilePerson(val identifier: String) : InternalEvent {
  override val occurredAt: LocalDateTime = LocalDateTime.now()
  override val type: String = EVENT_TYPE

  companion object {
    const val EVENT_TYPE = "court-appearance.reconcile.person"
  }
}
