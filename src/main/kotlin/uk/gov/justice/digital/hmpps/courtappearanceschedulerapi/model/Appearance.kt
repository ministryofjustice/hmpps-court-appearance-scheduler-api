package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import java.time.LocalDateTime
import java.util.*

data class Appearance(
  val id: UUID,
  val person: Person,
  val prison: Prison,
  val court: Court,
  val reason: AppearanceReason,
  val external: Boolean,
  val start: LocalDateTime,
  val end: LocalDateTime?,
  val comments: String?,
  val status: AppearanceStatus,
)

data class AppearanceStatus(val code: CourtAppearanceStatus.Code, val description: String)

fun CourtAppearanceStatus.asStatus() = AppearanceStatus(code, description)
