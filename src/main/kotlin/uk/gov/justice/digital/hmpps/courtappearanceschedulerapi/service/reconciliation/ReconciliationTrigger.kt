package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import io.sentry.Sentry
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.env.getProperty
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearanceReconcilePrison
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.InternalEventEmitter
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonregister.PrisonRegisterClient

@Conditional(ReconciliationActive::class)
@Service
class ReconciliationTrigger(
  private val prisonRegister: PrisonRegisterClient,
  private val iee: InternalEventEmitter,
) {
  @Scheduled(cron = $$"${service.reconciliation.cron}")
  fun reconciliation() {
    try {
      iee.publishInternalEvents(prisonRegister.findAllPrisons().map { CourtAppearanceReconcilePrison(it.code) })
    } catch (e: Exception) {
      Sentry.captureException(e)
    } finally {
      SchedulerContext.clear()
    }
  }
}

class ReconciliationActive : Condition {
  override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = context.environment.getProperty<String>("service.reconciliation.cron", "").isNotEmpty()
}
