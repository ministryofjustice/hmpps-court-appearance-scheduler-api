package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import java.time.LocalDateTime
import java.util.UUID

data class IntegrationMovement(
  val id: UUID,
  val scheduleId: UUID?,
  val personIdentifier: String,
  val prisonCode: String,
  val courtCode: String,
  val direction: CourtAppearanceMovement.Direction,
  val reason: IntegrationReason,
  val occurredAt: LocalDateTime,
  val comments: String?,
)
