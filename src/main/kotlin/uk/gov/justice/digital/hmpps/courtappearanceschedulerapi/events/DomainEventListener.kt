package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component

@Component
class DomainEventListener {

  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun handleDomainEvent(notification: Notification) {
    // TODO: handle events
  }
}
