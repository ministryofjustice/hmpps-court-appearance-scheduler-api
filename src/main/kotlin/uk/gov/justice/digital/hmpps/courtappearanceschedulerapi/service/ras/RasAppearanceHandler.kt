package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.ras

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getStatusByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.RasAppearanceDeleted
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.RasAppearanceEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.locationAt
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
    else -> handleUpsert(event)
  }

  private fun handleDelete(event: RasAppearanceDeleted) {
    appearanceRepository.findByExternalReference(event.externalReference())
      ?.also {
        if (it.movements.isEmpty()) {
          appearanceRepository.delete(it)
        } else {
          it.applyExternalIdentifiers(null, it.legacyId)
        }
      }
  }

  private fun handleUpsert(event: RasAppearanceEvent) {
    val ras = requireNotNull(rasClient.findCourtAppearanceSchedule(event.additionalInformation.courtAppearanceId))
    val mrm = prisonApi.movementsFor(event.getPersonIdentifier()).mostRecent()
    appearanceRepository.findByExternalReference(event.externalReference())?.also { cas ->
      if (cas.person.identifier != ras.personIdentifier) throw ConflictException("Court appearance person conflict")
      cas.reschedule(RescheduleAppearance(ras.start, cas.end))
      cas.relocate(RelocateAppearance(ras.courtCode))
      cas.recategorise(RecategoriseAppearance(ras.reason.code), reasonRepository::getReasonByCode)
      cas.applyComments(ChangeAppearanceComments(ras.comments))
      cas.calculateStatus(statusRepository::getStatusByCode, ras.start.isBefore(mrm?.movementDateTime ?: LocalDateTime.now()), ras.isDuplicate)
    } ?: run {
      appearanceRepository.save(ras.asEntity())
    }
  }

  private fun CourtAppearanceSchedule.asEntity(): CourtAppearance {
    val person = personSummaryService.getWithSave(personIdentifier)
    val movements = prisonApi.movementsFor(personIdentifier)
    val mrm = movements.mostRecent()
    val prisonCode = movements.locationAt(start)
    return CourtAppearance(
      person,
      prisonCode,
      courtCode,
      reasonRepository.getReasonByCode(reason.code),
      start,
      end,
      comments,
      externalReference,
      null,
    ).calculateStatus(statusRepository::getStatusByCode, start.isBefore(mrm?.movementDateTime ?: LocalDateTime.now()), isDuplicate)
  }
}
