package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.QueryHint
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.envers.Audited
import org.hibernate.jpa.HibernateHints
import org.hibernate.type.SqlTypes
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.QueryHints
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent
import java.util.*

@Audited
@Entity
@Table(name = "hmpps_domain_event")
class HmppsDomainEvent(
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "event")
  val event: DomainEvent<*>,

  @Id
  @Column(name = "id", nullable = false)
  val id: UUID = newUuid(),
) {
  @Version
  val version: Int? = null

  @Column(name = "event_type")
  val eventType: String = event.eventType

  @Column(name = "entity_id")
  val entityId: UUID = event.id

  var published: Boolean = false
}

interface HmppsDomainEventRepository : JpaRepository<HmppsDomainEvent, UUID> {
  @QueryHints(value = [QueryHint(name = HibernateHints.HINT_NATIVE_LOCK_MODE, value = "UPGRADE-SKIPLOCKED")])
  fun findByPublishedIsFalseOrderById(pageable: Pageable): List<HmppsDomainEvent>
}
