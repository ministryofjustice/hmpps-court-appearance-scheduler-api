package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.set
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.PrisonerMerged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.PrisonerMergedInformation

@Service
class PrisonerMergedHandler(
  private val transactionTemplate: TransactionTemplate,
  private val personSummaryService: PersonSummaryService,
) {
  fun handle(de: DomainEvent<PrisonerMergedInformation>) {
    val pmi = de.additionalInformation
    personSummaryService.findPersonSummary(pmi.removedNomsNumber)?.also { person ->
      SchedulerContext.get().copy(reason = PrisonerMerged.DESCRIPTION, source = DataSource.NOMIS).set()
      transactionTemplate.executeWithoutResult {
        personSummaryService.remove(person)
      }
    }
  }
}
