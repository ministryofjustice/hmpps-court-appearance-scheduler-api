package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.HmppsDomainEventRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceMovementRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceStatusRepository

@TestConfiguration
class TestConfig(
  private val transactionTemplate: TransactionTemplate,
  private val hmppsDomainEventRepository: HmppsDomainEventRepository,
  private val personSummaryRepository: PersonSummaryRepository,
  private val reasonRepository: CourtAppearanceReasonRepository,
  private val statusRepository: CourtAppearanceStatusRepository,
  private val appearanceRepository: CourtAppearanceRepository,
  private val movementRepository: CourtAppearanceMovementRepository,
) {
  @Bean
  fun hmppsDomainEventOperations(): HmppsDomainEventOperations = HmppsDomainEventOperationsImpl(transactionTemplate, hmppsDomainEventRepository)

  @Bean
  fun personSummaryOperations(): PersonSummaryOperations = PersonSummaryOperationsImpl(personSummaryRepository)

  @Bean
  fun courtAppearanceOperations(psOperations: PersonSummaryOperations): CourtAppearanceOperations = CourtAppearanceOperationsImpl(
    transactionTemplate,
    reasonRepository,
    statusRepository,
    appearanceRepository,
    psOperations,
  )

  @Bean
  fun courtMovementOperations(psOperations: PersonSummaryOperations): CourtMovementOperations = CourtMovementOperationsImpl(transactionTemplate, reasonRepository, movementRepository, psOperations)
}
