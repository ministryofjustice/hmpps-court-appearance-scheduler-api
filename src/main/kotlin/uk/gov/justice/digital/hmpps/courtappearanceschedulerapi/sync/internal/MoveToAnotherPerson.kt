package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.set
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovementRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PersonSummaryService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.MoveCourtEventRequest

@Transactional
@Service
class MoveToAnotherPerson(
  private val personSummaryService: PersonSummaryService,
  private val appearanceRepository: CourtAppearanceRepository,
  private val movementRepository: CourtAppearanceMovementRepository,
) {
  fun appearancesAndMovements(request: MoveCourtEventRequest) {
    SchedulerContext.get().copy(username = SYSTEM_USERNAME, reason = "Prisoner booking moved", source = DataSource.NOMIS).set()
    val moveTo = personSummaryService.getWithSave(request.toPersonIdentifier)
    appearanceRepository.findAllById(request.scheduleIds).forEach { sch ->
      check(request contains sch.person.identifier) { EXCEPTION_MESSAGE }
      sch.movePerson(moveTo)
      sch.movements.forEach { mov ->
        check(request contains mov.person.identifier) { EXCEPTION_MESSAGE }
        mov.movePerson(moveTo)
      }
    }
    movementRepository.findAllById(request.unscheduledMovementIds).forEach {
      it.movePerson(moveTo)
    }
    val appCount = { appearanceRepository.countByPersonIdentifier(request.fromPersonIdentifier) }
    val movementCount = { movementRepository.countByPersonIdentifier(request.fromPersonIdentifier) }
    if (appCount() == 0 && movementCount() == 0) {
      personSummaryService.findPersonSummary(request.fromPersonIdentifier)?.also(personSummaryService::remove)
    }
  }

  private infix fun MoveCourtEventRequest.contains(personIdentifier: String): Boolean = fromPersonIdentifier == personIdentifier || toPersonIdentifier == personIdentifier

  companion object {
    const val EXCEPTION_MESSAGE = "Assigned to the wrong person and cannot be moved"
  }
}
