package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "reconciliation_history")
final class ReconciliationHistory(
  @Column(name = "type", nullable = false)
  val type: String,

  @Column(name = "requested_on", nullable = false)
  val requestedOn: LocalDate = LocalDate.now(),

  @Id
  @Column(name = "id", nullable = false)
  val id: UUID = newUuid(),
) {
  @Version
  @Column(name = "version", nullable = false)
  var version: Int? = null
    private set
}

interface ReconciliationHistoryRepository : JpaRepository<ReconciliationHistory, UUID> {
  fun findByTypeAndRequestedOn(type: String, requestedOn: LocalDate): ReconciliationHistory?
}
