package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearancePushPerson
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.InternalEventEmitter

@Service
class PushCourtAppearanceData(
  private val iee: InternalEventEmitter,
  private val personSummaryRepository: PersonSummaryRepository,
) {
  fun toRemandAndSentencing() {
    iee.publishInternalEvents(personSummaryRepository.findAll().map { CourtAppearancePushPerson(it.identifier) })
  }
}
