package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ValidStartAndEnd
import java.time.LocalDate

@ValidStartAndEnd
@ValidDateRange(31)
data class CourtAppearanceSearchRequest(
  override val start: LocalDate,
  override val end: LocalDate,
  @Schema(requiredMode = NOT_REQUIRED)
  override val status: Set<CourtAppearanceStatus.Code> = emptySet(),
  @Schema(requiredMode = NOT_REQUIRED)
  override val reason: Set<String> = emptySet(),
  @Schema(requiredMode = NOT_REQUIRED)
  override val courtCodes: Set<String> = emptySet(),
  override val external: Boolean? = null,
  val query: String? = null,
  override val page: Int = 1,
  override val size: Int = 10,
  override val sort: String = CourtAppearance::start.name,
) : AppearanceSearchRequest
