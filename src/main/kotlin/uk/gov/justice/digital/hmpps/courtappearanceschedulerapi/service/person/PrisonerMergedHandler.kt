package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.set
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovementRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearanceMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.PrisonerMerged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.PrisonerMergedInformation
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.nomis.MigrationClient

@Service
class PrisonerMergedHandler(
  private val transactionTemplate: TransactionTemplate,
  private val appearanceRepository: CourtAppearanceRepository,
  private val movementRepository: CourtAppearanceMovementRepository,
  private val personSummaryService: PersonSummaryService,
  private val migrationClient: MigrationClient,
) {
  fun handle(de: DomainEvent<PrisonerMergedInformation>) {
    val pmi = de.additionalInformation
    personSummaryService.findPersonSummary(pmi.removedNomsNumber)?.also { person ->
      SchedulerContext.get().copy(reason = PrisonerMerged.DESCRIPTION, source = DataSource.NOMIS).set()
      transactionTemplate.executeWithoutResult {
        val toPerson = personSummaryService.getWithSave(pmi.nomsNumber)
        appearanceRepository.findAll(appearanceMatchesPersonIdentifier(pmi.removedNomsNumber, null))
          .forEach { it.movePerson(toPerson) }
        movementRepository.findAllByPersonIdentifier(pmi.removedNomsNumber).forEach { it.movePerson(toPerson) }
        personSummaryService.remove(person)
      }
      // migrationClient.requestRepair(pmi.removedNomsNumber)
    }
    // migrationClient.requestRepair(pmi.nomsNumber)
  }
}
