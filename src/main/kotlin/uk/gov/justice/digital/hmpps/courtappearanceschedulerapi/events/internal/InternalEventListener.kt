package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal

import io.awspring.cloud.sqs.annotation.SqsListener
import io.sentry.Sentry
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.Notification
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation.PushCourtAppearanceData
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation.PushPersonAppearanceData
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation.ReconcileActive
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation.ReconcilePerson
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation.ReconcilePrison

@Component
class InternalEventListener(
  private val jsonMapper: JsonMapper,
  private val active: ReconcileActive,
  private val prison: ReconcilePrison,
  private val person: ReconcilePerson,
  private val pushAll: PushCourtAppearanceData,
  private val pushPerson: PushPersonAppearanceData,
) {
  @SqsListener(
    "internalevents",
    factory = "hmppsQueueContainerFactoryProxy",
    maxConcurrentMessages = "20",
    maxMessagesPerPoll = "10",
  )
  fun handleInternalEvent(notification: Notification) {
    val event = jsonMapper.readValue<InternalEvent>(notification.message)
    try {
      when (event) {
        is CourtAppearanceReconcileActive -> active.reconcile()
        is CourtAppearanceReconcilePrison -> prison.reconcile(event.prisonCode)
        is CourtAppearanceReconcilePerson -> person.reconcile(event.identifier)

        is CourtAppearancePushAll -> pushAll.toRemandAndSentencing()
        is CourtAppearancePushPerson -> pushPerson.toRemandAndSentencing(event.identifier)
      }
    } catch (ex: Exception) {
      Sentry.captureException(ex)
      throw ex
    } finally {
      SchedulerContext.clear()
    }
  }
}
