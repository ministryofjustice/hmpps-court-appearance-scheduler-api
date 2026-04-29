package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEventMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.User
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

object SyncGenerator {
  fun courtEvent(
    scheduledPrisonCode: String = prisonCode(),
    scheduledCourtCode: String = courtCode(),
    type: String = "CRT",
    status: String = "SCH",
    date: LocalDate = LocalDate.now().plusDays(1),
    startTime: LocalTime = LocalTime.of(10, 0),
    commentText: String? = word(10) + " " + word(8),
    eventId: Long = newId(),
    externalReference: String? = null,
    dpsId: UUID? = null,
  ): CourtEvent = CourtEvent(dpsId, scheduledPrisonCode, scheduledCourtCode, eventId, date, startTime, type, status, commentText, externalReference)

  fun courtEventMovement(
    scheduleId: UUID? = null,
    bookingId: Long = newId(),
    sequenceNumber: Int = newId().toInt(),
    directionCode: String = "OUT",
    fromAgencyId: String = prisonCode(),
    toAgencyId: String = courtCode(),
    reasonCode: String = "CRT",
    date: LocalDate = LocalDate.now().plusDays(1),
    time: LocalTime = LocalTime.of(10, 0),
    commentText: String? = word(10) + " " + word(8),
    dpsId: UUID? = null,
  ): CourtEventMovement = CourtEventMovement(
    dpsId,
    scheduleId,
    bookingId,
    sequenceNumber,
    date,
    time,
    reasonCode,
    directionCode,
    fromAgencyId,
    toAgencyId,
    commentText,
  )

  fun syncUser(username: String = username(), activeCaseloadId: String? = null): User = User(username, activeCaseloadId)
}
