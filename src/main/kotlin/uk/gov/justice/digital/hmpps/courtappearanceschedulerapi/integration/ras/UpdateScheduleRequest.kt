package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import java.time.LocalDateTime

data class UpdateScheduleRequest(
  val prisonCode: String,
  val courtCode: String,
  val reasonCode: String,
  val start: LocalDateTime,
  val comments: String?,
)

fun CourtAppearance.asUpdateRequest() = UpdateScheduleRequest(
  // use person's current prison for RaS
  person.prisonCode ?: prisonCode,
  courtCode,
  reason.code,
  start,
  comments,
)
