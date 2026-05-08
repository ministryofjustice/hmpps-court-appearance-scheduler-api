package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration

import java.time.LocalDateTime
import java.util.UUID

data class IntegrationAppearance(
  val id: UUID,
  val personIdentifier: String,
  val prisonCode: String,
  val courtCode: String,
  val reason: IntegrationReason,
  val start: LocalDateTime,
  val end: LocalDateTime?,
  val comments: String?,
)
