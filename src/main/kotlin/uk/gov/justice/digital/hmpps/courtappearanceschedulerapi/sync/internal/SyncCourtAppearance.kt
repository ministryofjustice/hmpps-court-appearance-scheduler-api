package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.set
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceEntity.COURT_APPEARANCE
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService.REMAND_AND_SENTENCING
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getStatusByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.RemandAndSentencingClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PersonSummaryService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.SyncCourtEvent
import java.util.UUID

@Transactional
@Service
class SyncCourtAppearance(
  private val rasClient: RemandAndSentencingClient,
  private val personSummaryService: PersonSummaryService,
  private val reasonRepository: CourtAppearanceReasonRepository,
  private val statusRepository: CourtAppearanceStatusRepository,
  private val appearanceRepository: CourtAppearanceRepository,
) {

  fun sync(personIdentifier: String, request: SyncCourtEvent): ReferenceId {
    with(request) {
      SchedulerContext.get()
        .copy(requestAt = occurredAt, username = user.username, caseloadId = user.activeCaseloadId)
        .set()
    }
    val rasScheduleInfo = request.courtEvent.externalReferenceUrn?.uuid?.let(rasClient::findCourtAppearanceSchedule)
    val person = personSummaryService.getWithSave(personIdentifier)
    val appearance = (
      request.courtEvent.dpsId?.let { appearanceRepository.findByIdOrNull(it) }
        ?: request.courtEvent.externalReferenceUrn?.let { appearanceRepository.findByExternalReference(it) }
        ?: request.courtEvent.eventId?.let { appearanceRepository.findByLegacyId(it) }
      )?.updateFrom(
      person,
      request.courtEvent,
      reasonRepository::getReasonByCode,
      statusRepository::getStatusByCode,
      rasScheduleInfo,
    )
      ?: appearanceRepository.save(
        request.courtEvent.asEntity(
          person,
          reasonRepository::getReasonByCode,
          statusRepository::getStatusByCode,
          rasScheduleInfo,
        ),
      )
    return ReferenceId(appearance.id)
  }

  fun delete(id: UUID): Boolean = appearanceRepository.findByIdOrNull(id)?.let { appearance ->
    SchedulerContext.get().copy(username = SYSTEM_USERNAME).set()
    if (appearance.movements.isEmpty()) {
      appearanceRepository.delete(appearance)
      true
    } else {
      if (appearance.isRasAppearance()) {
        appearance.applyExternalIdentifiers(null, appearance.legacyId)
      }
      false
    }
  } ?: true

  private fun CourtAppearance.isRasAppearance(): Boolean = externalReference?.takeIf { it.service == REMAND_AND_SENTENCING && it.entity == COURT_APPEARANCE } != null
}
