package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import io.sentry.Sentry
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReconciliationHistory
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReconciliationHistoryRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearanceReconcileEnhanced
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearanceReconcilePerson
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.InternalEventEmitter
import java.time.LocalDate
import kotlin.streams.asSequence

@Service
class ReconcileEnhanced(
  private val transactionTemplate: TransactionTemplate,
  private val rhr: ReconciliationHistoryRepository,
  private val psr: PersonSummaryRepository,
  private val iee: InternalEventEmitter,
) {
  fun reconcile() {
    try {
      if (rhr.findByTypeAndRequestedOn(CourtAppearanceReconcileEnhanced.EVENT_TYPE, LocalDate.now()) != null) return
      transactionTemplate.execute {
        rhr.save(ReconciliationHistory(CourtAppearanceReconcileEnhanced.EVENT_TYPE))
      }
      transactionTemplate.executeWithoutResult {
        psr.findIdentifiers().map { CourtAppearanceReconcilePerson(it) }
          .asSequence().chunked(10).forEach(iee::publishInternalEvents)
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
