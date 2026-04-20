package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEventMovement

infix fun CourtAppearance.verifyAgainst(request: CourtEvent) {
  assertThat(prisonCode).isEqualTo(request.scheduledPrisonCode)
  assertThat(courtCode).isEqualTo(request.scheduledCourtCode)
  assertThat(legacyId).isEqualTo(request.eventId)
  assertThat(start.toLocalDate()).isEqualTo(request.date)
  assertThat(reason.code).isEqualTo(request.type)
  assertThat(comments).isEqualTo(request.commentText)
}

infix fun CourtAppearanceMovement.verifyAgainst(request: CourtEventMovement) {
  assertThat(legacyId).isEqualTo("${request.bookingId}_${request.sequenceNumber}")
  assertThat(courtCode).isEqualTo(request.courtCode)
  assertThat(prisonCode).isEqualTo(request.prisonCode)
  assertThat(comments).isEqualTo(request.commentText)
  assertThat(reason.code).isEqualTo(request.reasonCode)
  assertThat(occurredAt.toLocalDate()).isEqualTo(request.date)
}
