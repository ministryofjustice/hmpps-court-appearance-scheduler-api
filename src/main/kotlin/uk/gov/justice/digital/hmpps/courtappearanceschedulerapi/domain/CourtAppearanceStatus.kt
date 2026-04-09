package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.exception.NotFoundException
import java.util.*

@Immutable
@Entity
@Table(name = "court_appearance_status")
class CourtAppearanceStatus(

  @Enumerated(EnumType.STRING)
  @Column(name = "code", nullable = false)
  val code: Code,

  @Column(name = "description", nullable = false)
  val description: String,

  @Column(name = "sequence_number", nullable = false)
  val sequenceNumber: Int,

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  val id: UUID,
) {
  enum class Code {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    EXPIRED,
    ;

    companion object {
      fun of(code: String): Code = entries.firstOrNull { it.name.equals(code, true) }
        ?: throw IllegalArgumentException("Invalid Court Appearance Status Code")
    }
  }
}

interface CourtAppearanceStatusRepository : JpaRepository<CourtAppearanceStatus, UUID> {
  fun findByCode(code: CourtAppearanceStatus.Code): CourtAppearanceStatus?
}

fun CourtAppearanceStatusRepository.getStatusByCode(code: CourtAppearanceStatus.Code): CourtAppearanceStatus = findByCode(code) ?: throw NotFoundException("Status not found")
