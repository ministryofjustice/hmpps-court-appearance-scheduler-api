package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AppearanceReason
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Court
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Person
import java.time.LocalDateTime
import java.util.UUID

data class CourtAppearanceSearchResponse(
  override val content: List<CourtAppearanceResult>,
  override val metadata: PageMetadata,
) : PagedResponse<CourtAppearanceResult>

data class CourtAppearanceResult(
  val id: UUID,
  val person: Person,
  val court: Court,
  val reason: AppearanceReason,
  val external: Boolean,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val comments: String?,
  val status: AppearanceStatus,
)
