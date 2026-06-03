package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEventMovement
import java.time.temporal.ChronoUnit

infix fun CourtAppearance.verifyAgainst(request: CourtEvent) {
  assertThat(prisonCode).isEqualTo(request.scheduledPrisonCode)
  assertThat(courtCode).isEqualTo(request.scheduledCourtCode)
  assertThat(legacyId).isEqualTo(request.eventId)
  assertThat(externalReference).isEqualTo(request.externalReferenceUrn)
  assertThat(start.toLocalDate()).isEqualTo(request.start.toLocalDate())
  assertThat(reason.code).isEqualTo(request.type)
  assertThat(comments).isEqualTo(request.commentText)
  request.isExternal?.also { assertThat(external).isEqualTo(it) }
}

infix fun CourtAppearanceMovement.verifyAgainst(request: CourtEventMovement) {
  legacyId?.also {
    assertThat(it).isEqualTo("${request.bookingId}_${request.sequenceNumber}")
  } ?: run {
    assertThat(request.bookingId).isNull()
    assertThat(request.sequenceNumber).isNull()
  }
  assertThat(courtCode).isEqualTo(request.courtCode)
  assertThat(prisonCode).isEqualTo(request.prisonCode)
  assertThat(comments).isEqualTo(request.commentText)
  assertThat(reason.code).isEqualTo(request.reasonCode)
  assertThat(occurredAt.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(request.occurredAt.truncatedTo(ChronoUnit.SECONDS))
}
