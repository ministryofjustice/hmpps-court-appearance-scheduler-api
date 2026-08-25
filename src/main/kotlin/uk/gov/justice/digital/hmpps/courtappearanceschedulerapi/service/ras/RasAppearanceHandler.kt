package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.ras

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.getStatusByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.RasAppearanceDeleted
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.RasAppearanceEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.RasAppearanceInserted
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.RasAppearanceUpdated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.mostRecent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.RemandAndSentencingClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ChangeAppearanceComments
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RecategoriseAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RelocateAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RescheduleAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PersonSummaryService
import java.time.LocalDateTime

@Transactional
@Service
class RasAppearanceHandler(
  private val rasClient: RemandAndSentencingClient,
  private val prisonApi: PrisonApiClient,
  private val personSummaryService: PersonSummaryService,
  private val appearanceRepository: CourtAppearanceRepository,
  private val reasonRepository: CourtAppearanceReasonRepository,
  private val statusRepository: CourtAppearanceStatusRepository,
) {
  fun handle(event: RasAppearanceEvent) = when (event) {
    is RasAppearanceDeleted -> handleDelete(event)
    is RasAppearanceInserted -> createAppearance(event)
    is RasAppearanceUpdated -> handleUpdate(event)
  }

  private fun handleDelete(event: RasAppearanceDeleted) {
    appearanceRepository.findByExternalReference(event.externalReference())
      ?.also {
        it.movements.toList().forEach(it::removeMovement)
        appearanceRepository.delete(it)
      }
  }

  private fun createAppearance(event: RasAppearanceInserted) {
    rasClient.findCourtAppearanceSchedule(event.additionalInformation.courtAppearanceId)?.also { ras ->
      appearanceRepository.save(ras.asEntity())
    }
  }

  private fun handleUpdate(event: RasAppearanceEvent) {
    rasClient.findCourtAppearanceSchedule(event.additionalInformation.courtAppearanceId)?.also { ras ->
      val movements = prisonApi.movementsFor(event.getPersonIdentifier())
      val mrm = movements.mostRecent()
      appearanceRepository.findByExternalReference(event.externalReference())?.also { cas ->
        cas.reschedule(RescheduleAppearance(ras.start, cas.end))
        cas.relocate(RelocateAppearance(ras.courtCode))
        cas.recategorise(RecategoriseAppearance(ras.reason.code), reasonRepository::getReasonByCode)
        cas.applyComments(ChangeAppearanceComments(ras.comments))
        cas.calculateStatus(
          statusRepository::getStatusByCode,
          ras.start.isBefore(mrm?.movementDateTime ?: LocalDateTime.now()),
          ras.isDuplicate,
        )
      }
    }
  }

  private fun CourtAppearanceSchedule.asEntity(): CourtAppearance {
    val person = personSummaryService.getWithSave(personIdentifier)
    val movements = prisonApi.movementsFor(personIdentifier)
    val mrm = movements.mostRecent()
    return CourtAppearance(
      person,
      courtCode,
      reasonRepository.getReasonByCode(reason.code),
      start,
      end,
      comments,
      externalReference,
      null,
    ).calculateStatus(
      statusRepository::getStatusByCode,
      start.isBefore(mrm?.movementDateTime ?: LocalDateTime.now()),
      isDuplicate,
    )
  }
}
