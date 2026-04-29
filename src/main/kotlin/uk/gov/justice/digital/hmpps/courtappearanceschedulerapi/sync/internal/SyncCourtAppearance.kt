package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.set
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getStatusByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PersonSummaryService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.SyncCourtEvent
import java.util.UUID

@Transactional
@Service
class SyncCourtAppearance(
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
    val person = personSummaryService.getWithSave(personIdentifier)
    val appearance = (
      request.courtEvent.dpsId?.let { appearanceRepository.findByIdOrNull(it) }
        ?: request.courtEvent.eventId?.let { appearanceRepository.findByLegacyId(it) }
        ?: request.courtEvent.externalReferenceUrn?.let { appearanceRepository.findByExternalReference(it) }
      )?.updateFrom(person, request.courtEvent, reasonRepository::getReasonByCode, statusRepository::getStatusByCode)
      ?: appearanceRepository.save(
        request.courtEvent.asEntity(
          person,
          reasonRepository::getReasonByCode,
          statusRepository::getStatusByCode,
        ),
      )
    return ReferenceId(appearance.id)
  }

  fun delete(id: UUID) {
    appearanceRepository.findByIdOrNull(id)?.also { appearance ->
      SchedulerContext.get().copy(username = SYSTEM_USERNAME).set()
      if (appearance.movements.isEmpty()) {
        appearanceRepository.delete(appearance)
      } else {
        throw ConflictException("Cannot delete a scheduled appearance with a recorded movement.")
      }
    }
  }
}
