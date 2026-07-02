package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import io.sentry.Sentry
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReconciliationHistory
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReconciliationHistoryRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearanceReconcileActive
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearanceReconcilePrison
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.InternalEventEmitter
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonregister.PrisonRegisterClient
import java.time.LocalDate

@Service
class ReconcileActive(
  private val transactionTemplate: TransactionTemplate,
  private val rhr: ReconciliationHistoryRepository,
  private val prisonRegister: PrisonRegisterClient,
  private val iee: InternalEventEmitter,
) {
  fun reconcile() {
    try {
      if (rhr.findByTypeAndRequestedOn(CourtAppearanceReconcileActive.EVENT_TYPE, LocalDate.now()) != null) return
      transactionTemplate.execute {
        rhr.save(ReconciliationHistory(CourtAppearanceReconcileActive.EVENT_TYPE))
      }
      iee.publishInternalEvents(prisonRegister.findAllPrisons().map { CourtAppearanceReconcilePrison(it.code) })
    } catch (_: DataIntegrityViolationException) {
      // another pod started the job already
    } catch (e: Exception) {
      Sentry.captureException(e)
    } finally {
      SchedulerContext.clear()
    }
  }
}
