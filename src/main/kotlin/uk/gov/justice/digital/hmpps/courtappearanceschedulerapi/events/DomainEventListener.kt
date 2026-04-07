package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events

import io.awspring.cloud.sqs.annotation.SqsListener
import io.sentry.Sentry
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PersonUpdatedHandler
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PrisonerMergedHandler

@Component
class DomainEventListener(
  private val jsonMapper: JsonMapper,
  private val person: PersonUpdatedHandler,
  private val merged: PrisonerMergedHandler,
) {

  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun handleDomainEvent(notification: Notification) {
    try {
      when (notification.eventType) {
        PrisonerUpdated.EVENT_TYPE -> person.handle(jsonMapper.readValue(notification.message))
        PrisonerMerged.EVENT_TYPE -> merged.handle(jsonMapper.readValue(notification.message))
      }
    } catch (ex: Exception) {
      Sentry.captureException(ex)
      throw ex
    } finally {
      SchedulerContext.clear()
    }
  }
}
