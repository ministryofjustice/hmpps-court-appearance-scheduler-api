package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.TRUE
import java.time.LocalDateTime

data class AuditHistory(val content: List<AuditedAction>)

data class AuditedAction(
  val user: User,
  val occurredAt: LocalDateTime,
  val domainEvents: List<String>,
  val reason: String?,
  val changes: List<Change>,
) {
  data class User(val username: String, val name: String)
  data class Change(
    val propertyName: String,
    @Schema(additionalProperties = TRUE) val previous: Any?,
    @Schema(additionalProperties = TRUE) val change: Any?,
  )
}
