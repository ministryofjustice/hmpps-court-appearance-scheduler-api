package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.set
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovementRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getStatusByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PersonSummaryService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.SyncCourtEventMovement
import java.util.UUID

@Transactional
@Service
class SyncCourtMovement(
  private val personSummaryService: PersonSummaryService,
  private val reasonRepository: CourtAppearanceReasonRepository,
  private val statusRepository: CourtAppearanceStatusRepository,
  private val appearanceRepository: CourtAppearanceRepository,
  private val movementRepository: CourtAppearanceMovementRepository,
) {
  fun sync(personIdentifier: String, request: SyncCourtEventMovement): ReferenceId {
    with(request) {
      SchedulerContext.get()
        .copy(requestAt = occurredAt, username = user.username, caseloadId = user.activeCaseloadId)
        .set()
    }
    val person = personSummaryService.getWithSave(personIdentifier)
    val schedule = request.movement.scheduleId?.let { appearanceRepository.getAppearance(it) }
    val movement = (
      request.movement.dpsId?.let { movementRepository.findByIdOrNull(it) }
        ?: request.movement.legacyId?.let { movementRepository.findByLegacyId(it) }
      )
      ?.updateFrom(person, schedule, request.movement, reasonRepository::getReasonByCode, statusRepository::getStatusByCode)
      ?: movementRepository.save(
        request.movement.asEntity(person, schedule, reasonRepository::getReasonByCode, statusRepository::getStatusByCode),
      )
    return ReferenceId(movement.id)
  }

  fun delete(id: UUID) {
    movementRepository.findByIdOrNull(id)?.also {
      SchedulerContext.get().copy(username = SYSTEM_USERNAME).set()
      it.courtAppearance?.removeMovement(it)?.calculateStatus(statusRepository::getStatusByCode)
      movementRepository.delete(it)
    }
  }
}
