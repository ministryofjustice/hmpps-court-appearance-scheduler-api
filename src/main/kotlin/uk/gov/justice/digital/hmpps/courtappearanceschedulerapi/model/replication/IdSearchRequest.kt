package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.replication

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Prison
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.StartAndEnd
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ValidDateRange
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ValidStartAndEnd
import java.time.LocalDate

@ValidStartAndEnd
@ValidDateRange(92)
data class IdSearchRequest(
  @Parameter(description = "Start date (inclusive) of the window to retrieve ids for.", required = true)
  override val start: LocalDate,
  @Parameter(description = "End date (inclusive) of the window to retrieve ids for.", required = true)
  override val end: LocalDate,
  @Parameter(description = "Optional filter to restrict response to a given prison", example = "MDI")
  @Pattern(regexp = Prison.PATTERN, message = "Prison code should match `${Prison.PATTERN}`.")
  val prisonCode: String?,
  @Parameter(description = "Optional filter to restrict response to a given person in prison", example = "A1234BC")
  @Pattern(regexp = Prisoner.PATTERN, message = "Person identifier should match `${Prisoner.PATTERN}`.")
  val personIdentifier: String?,
  @Parameter(description = "The page to request, starting at 1.", example = "1", required = true)
  @Min(value = 1, message = "Page number must be at least 1.")
  val page: Int,
  @Parameter(description = "The page size to request. If not provided a default of 1000 is used.", example = "1000")
  @Min(value = 1, message = "Page size must be at least 1.")
  @Max(value = 10000, message = "Page size must be less than 10000.")
  val size: Int = 1000,
) : StartAndEnd<LocalDate> {
  fun pageable(): Pageable = PageRequest.of(page - 1, size)
}
