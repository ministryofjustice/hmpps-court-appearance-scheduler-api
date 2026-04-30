package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ValidStartAndEnd
import java.time.LocalDate

@ValidStartAndEnd
data class PersonAppearanceSearchRequest(
  override val start: LocalDate? = null,
  override val end: LocalDate? = null,
  override val status: Set<CourtAppearanceStatus.Code> = emptySet(),
  override val reason: Set<String> = emptySet(),
  override val courtCodes: Set<String> = emptySet(),
  override val external: Boolean? = null,
  override val page: Int = 1,
  override val size: Int = 10,
  override val sort: String = CourtAppearance::start.name,
) : AppearanceSearchRequest
