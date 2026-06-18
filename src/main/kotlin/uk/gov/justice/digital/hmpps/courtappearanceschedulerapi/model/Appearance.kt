package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AppearanceOrigin.OriginSource
import java.time.LocalDateTime
import java.util.UUID

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
  val origin: AppearanceOrigin?,
)

data class AppearanceStatus(val code: CourtAppearanceStatus.Code, val description: String)
data class AppearanceOrigin(val source: OriginSource, val id: UUID) {
  data class OriginSource(val code: String, val name: String)
}

fun CourtAppearanceStatus.asStatus() = AppearanceStatus(code, description)
fun ExternalReference.asAppearanceOrigin() = AppearanceOrigin(OriginSource(service.code, service.description), uuid)
