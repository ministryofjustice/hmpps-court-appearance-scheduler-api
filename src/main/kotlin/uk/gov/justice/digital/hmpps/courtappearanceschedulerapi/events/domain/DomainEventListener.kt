package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain

import io.awspring.cloud.sqs.annotation.SqsListener
import io.sentry.Sentry
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.set
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.Notification
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PersonUpdatedHandler
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PrisonerMergedHandler
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PrisonerReceivedHandler
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.ras.RasAppearanceHandler
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal.CourtAppearanceClonedHandler
import java.time.LocalDateTime

@Component
class DomainEventListener(
  private val jsonMapper: JsonMapper,
  private val person: PersonUpdatedHandler,
  private val merged: PrisonerMergedHandler,
  private val received: PrisonerReceivedHandler,
  private val rasHandler: RasAppearanceHandler,
  private val cacHandler: CourtAppearanceClonedHandler,
) {

  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun handleDomainEvent(notification: Notification) {
    SchedulerContext.get().copy(requestAt = LocalDateTime.now()).set()
    try {
      when (notification.eventType) {
        PrisonerUpdated.EVENT_TYPE -> person.handle(jsonMapper.readValue(notification.message))
        PrisonerMerged.EVENT_TYPE -> merged.handle(jsonMapper.readValue(notification.message))
        PrisonerReceived.EVENT_TYPE -> received.handle(jsonMapper.readValue(notification.message))
        RasAppearanceDeleted.EVENT_TYPE, RasAppearanceInserted.EVENT_TYPE, RasAppearanceUpdated.EVENT_TYPE ->
          rasHandler.handle(jsonMapper.readValue(notification.message))

        CourtAppearanceCloned.EVENT_TYPE -> cacHandler.handle(jsonMapper.readValue(notification.message))
      }
    } catch (ex: Exception) {
      Sentry.captureException(ex)
      throw ex
    } finally {
      SchedulerContext.clear()
    }
  }
}
