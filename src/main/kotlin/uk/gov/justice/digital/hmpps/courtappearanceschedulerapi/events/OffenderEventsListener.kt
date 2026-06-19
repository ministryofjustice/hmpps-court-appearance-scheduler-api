package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events

import io.awspring.cloud.sqs.annotation.SqsListener
import io.sentry.Sentry
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.externalmovements.ExternalMovementHandler
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.externalmovements.ExternalMovementRecordedEvent

@Component
class OffenderEventsListener(
  private val jsonMapper: JsonMapper,
  private val emHandler: ExternalMovementHandler,
) {
  @SqsListener("offendereventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun receive(notification: Notification) {
    try {
      when (notification.eventType) {
        ExternalMovementRecordedEvent.EVENT_TYPE ->
          emHandler.handle(jsonMapper.readValue<ExternalMovementRecordedEvent>(notification.message))
      }
    } catch (ex: Exception) {
      Sentry.captureException(ex)
      throw ex
    } finally {
      SchedulerContext.clear()
    }
  }
}
