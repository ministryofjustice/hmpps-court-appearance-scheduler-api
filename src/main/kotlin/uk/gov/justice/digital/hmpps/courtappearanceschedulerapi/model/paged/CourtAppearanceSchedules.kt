package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AppearanceReason
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Court
import java.time.LocalDateTime
import java.util.*

data class CourtAppearanceSchedules(
  override val content: List<CourtAppearanceSchedule>,
  override val metadata: PageMetadata,
) : PagedResponse<CourtAppearanceSchedule>

data class CourtAppearanceSchedule(
  val id: UUID,
  val personIdentifier: String,
  val court: Court,
  val reason: AppearanceReason,
  val external: Boolean,
  val start: LocalDateTime,
  val end: LocalDateTime?,
  val status: AppearanceStatus,
  val detail: Detail,
) {
  data class Detail(
    val uiUrl: String,
    val requiredRoles: Set<String>,
  ) {
    companion object {
      fun buildUiUrl(uiBaseUrl: String, appearanceId: UUID): String = "$uiBaseUrl/court-appearances/$appearanceId"
    }
  }
}
