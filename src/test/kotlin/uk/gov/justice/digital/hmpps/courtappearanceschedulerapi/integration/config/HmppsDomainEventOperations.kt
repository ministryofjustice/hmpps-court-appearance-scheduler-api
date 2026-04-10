package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.HmppsDomainEventRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent
import java.util.*

interface HmppsDomainEventOperations {
  fun givenHmppsDomainEvent(event: DomainEvent<*>, entityId: UUID): HmppsDomainEvent
  fun findHmppsDomainEvent(id: UUID): HmppsDomainEvent?
  fun markAllAsPublished()
}

class HmppsDomainEventOperationsImpl(
  private val transactionTemplate: TransactionTemplate,
  private val hmppsDomainEventRepository: HmppsDomainEventRepository,
) : HmppsDomainEventOperations {
  override fun givenHmppsDomainEvent(event: DomainEvent<*>, entityId: UUID): HmppsDomainEvent = hmppsDomainEventRepository.save(HmppsDomainEvent(event, entityId))

  override fun findHmppsDomainEvent(id: UUID): HmppsDomainEvent? = hmppsDomainEventRepository.findByIdOrNull(id)
  override fun markAllAsPublished() = transactionTemplate.executeWithoutResult {
    hmppsDomainEventRepository.findAll().forEach { it.apply { published = true } }
  }
}
