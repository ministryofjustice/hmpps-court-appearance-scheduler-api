package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import io.sentry.Sentry
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReconciliationHistory
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReconciliationHistoryRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearancePushAll
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearancePushPerson
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.InternalEventEmitter
import java.time.LocalDate
import kotlin.streams.asSequence

@Service
class PushCourtAppearanceData(
  private val iee: InternalEventEmitter,
  private val transactionTemplate: TransactionTemplate,
  private val rhr: ReconciliationHistoryRepository,
  private val personSummaryRepository: PersonSummaryRepository,
) {
  fun toRemandAndSentencing() {
    try {
      if (rhr.findByTypeAndRequestedOn(CourtAppearancePushAll.EVENT_TYPE, LocalDate.now()) != null) return
      transactionTemplate.execute {
        rhr.save(ReconciliationHistory(CourtAppearancePushAll.EVENT_TYPE))
      }
      transactionTemplate.executeWithoutResult {
        personSummaryRepository.findIdentifiers().map { CourtAppearancePushPerson(it) }
          .asSequence().chunked(10).forEach { iee.publishInternalEvents(it) }
      }
    } catch (_: DataIntegrityViolationException) {
      // another pod started the job already
    } catch (e: Exception) {
      Sentry.captureException(e)
    } finally {
      SchedulerContext.clear()
    }
  }
}
