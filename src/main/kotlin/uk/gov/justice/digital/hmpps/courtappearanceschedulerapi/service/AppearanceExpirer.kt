package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service

import io.sentry.Sentry
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.env.getProperty
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatusRepository
import java.time.LocalDate

@Transactional
@Service
class AppearanceExpirer(
  private val statusRepository: CourtAppearanceStatusRepository,
  private val appearanceRepository: CourtAppearanceRepository,
) {
  fun expireScheduledAppearances() {
    val statuses = statusRepository.findAll().associateBy { it.code }
    val statusProvider = { code: CourtAppearanceStatus.Code -> requireNotNull(statuses[code]) }
    val scheduled = statusProvider(CourtAppearanceStatus.Code.SCHEDULED)
    appearanceRepository.findByStatusIdAndStartBefore(scheduled.id, LocalDate.now().atStartOfDay())
      .takeIf { it.isNotEmpty() }
      ?.forEach { it.calculateStatus(statusProvider) }
  }
}

@Conditional(PollExpiredAppearanceCondition::class)
@Service
class AppearanceExpiringPoller(private val appearanceExpirer: AppearanceExpirer) {
  @Scheduled(cron = $$"${service.appearance-expiration.cron}")
  fun recalculatePastAppearances() {
    try {
      RetryTemplate().apply {
        setRetryPolicy(SimpleRetryPolicy().apply { maxAttempts = 3 })
        setBackOffPolicy(ExponentialBackOffPolicy().apply { initialInterval = 1000L })
      }.execute<Unit, RuntimeException> {
        appearanceExpirer.expireScheduledAppearances()
      }
    } catch (e: Exception) {
      Sentry.captureException(e)
    } finally {
      SchedulerContext.clear()
    }
  }
}

class PollExpiredAppearanceCondition : Condition {
  override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = context.environment.getProperty<String>("service.appearance-expiration.cron", "").isNotBlank()
}
