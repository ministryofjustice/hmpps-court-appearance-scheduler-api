package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import io.sentry.Sentry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearanceReconcileActive
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearanceReconcileEnhanced
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.InternalEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.InternalEventEmitter

@Service
class ReconciliationTrigger(
  private val iee: InternalEventEmitter,
) {
  @Scheduled(cron = $$"${service.reconciliation.cron:-}")
  fun activeReconciliation() {
    trigger(CourtAppearanceReconcileActive())
  }

  @Scheduled(cron = $$"${service.enhanced-reconciliation.cron:-}")
  fun enhancedReconciliation() {
    trigger(CourtAppearanceReconcileEnhanced())
  }

  private fun trigger(event: InternalEvent) {
    try {
      iee.publishInternalEvents(setOf(event))
    } catch (e: Exception) {
      Sentry.captureException(e)
    } finally {
      SchedulerContext.clear()
    }
  }
}
