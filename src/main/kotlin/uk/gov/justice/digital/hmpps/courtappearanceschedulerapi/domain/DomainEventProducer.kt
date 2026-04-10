package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent
import java.util.*

interface DomainEventProducer : Identifiable {
  fun initialEvents(): Set<DomainEventPublication> = setOf()

  fun domainEvents(): Set<DomainEventPublication> = setOf()

  fun deletionEvents(): Set<DomainEventPublication> = setOf()
}

data class DomainEventPublication(val event: DomainEvent<*>, val entityId: UUID, val publish: Boolean = true)

fun DomainEvent<*>.publication(entityId: UUID, publishSupplier: (DomainEvent<*>) -> Boolean = { true }) = DomainEventPublication(this, entityId, publishSupplier(this))
