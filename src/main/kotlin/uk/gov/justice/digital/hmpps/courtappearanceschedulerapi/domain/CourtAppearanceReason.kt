package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.exception.NotFoundException
import java.util.*

@Immutable
@Entity
@Table(name = "court_appearance_reason")
class CourtAppearanceReason(

  @Column(name = "code", nullable = false)
  val code: String,

  @Column(name = "description", nullable = false)
  val description: String,

  @Column(name = "sequence_number", nullable = false)
  val sequenceNumber: Int,

  @Column(name = "active", nullable = false)
  val active: Boolean,

  @Column(name = "external", nullable = false)
  val external: Boolean,

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  val id: UUID,
)

interface CourtAppearanceReasonRepository : JpaRepository<CourtAppearanceReason, UUID> {
  fun findByCode(code: String): CourtAppearanceReason?
}

fun CourtAppearanceReasonRepository.getReasonByCode(code: String) = findByCode(code) ?: throw NotFoundException("Reason not found")
