package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync

import java.time.LocalDateTime

data class SyncCourtEvent(
  val occurredAt: LocalDateTime,
  val user: User,
  val courtEvent: CourtEvent,
)

data class SyncCourtEventMovement(
  val occurredAt: LocalDateTime,
  val user: User,
  val movement: CourtEventMovement,
)
