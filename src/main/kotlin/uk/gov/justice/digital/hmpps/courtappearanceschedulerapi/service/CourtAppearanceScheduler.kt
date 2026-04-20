package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReasonProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.StatusProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getStatusByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ScheduleCourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PersonSummaryService

@Transactional
@Service
class CourtAppearanceScheduler(
  private val personService: PersonSummaryService,
  private val appearanceRepository: CourtAppearanceRepository,
  private val reasonRepository: CourtAppearanceReasonRepository,
  private val statusRepository: CourtAppearanceStatusRepository,
) {
  fun singleAppearance(personIdentifier: String, request: ScheduleCourtAppearance): ReferenceId {
    val person = personService.getWithSave(personIdentifier)
    val saved = request.persistable(person, reasonRepository::getReasonByCode, statusRepository::getStatusByCode)
      .also { appearanceRepository.save(it) }
    return ReferenceId(saved.id)
  }

  private fun ScheduleCourtAppearance.persistable(person: PersonSummary, reason: ReasonProvider, status: StatusProvider) = CourtAppearance(person, prisonCode, courtCode, reason(reasonCode), start, end, comments, null)
    .calculateStatus(status)
}
