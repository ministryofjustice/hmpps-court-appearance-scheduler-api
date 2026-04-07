package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Audited
@Entity
@Table(name = "court_appearance")
final class CourtAppearance(
  person: PersonSummary,
  prisonCode: String,
  courtCode: String,
  reason: CourtAppearanceReason,
  start: LocalDateTime,
  end: LocalDateTime?,
  comments: String?,
  legacyId: Long?,
  @Id
  @Column(name = "id", nullable = false)
  val id: UUID = newUuid(),
) {
  @Version
  @Column(name = "version", nullable = false)
  var version: Int? = null
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "person_identifier", nullable = false)
  var person: PersonSummary = person
    private set

  @Size(max = 6)
  @NotNull
  @Column(name = "prison_code", nullable = false, length = 6)
  var prisonCode: String = prisonCode
    private set

  @Size(max = 6)
  @NotNull
  @Column(name = "court_code", nullable = false, length = 6)
  var courtCode: String = courtCode
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "reason_id", nullable = false)
  var reason: CourtAppearanceReason = reason
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  lateinit var status: CourtAppearanceStatus
    private set

  @NotNull
  @Column(name = "external", nullable = false)
  var external: Boolean = reason.external
    private set

  @NotNull
  @Column(name = "start", nullable = false)
  var start: LocalDateTime = start
    private set

  @Column(name = "end")
  var end: LocalDateTime? = end
    private set

  @Column(name = "comments", length = Integer.MAX_VALUE)
  var comments: String? = comments
    private set

  @Column(name = "legacy_id")
  var legacyId: Long? = legacyId
    private set

  @Fetch(FetchMode.JOIN)
  @OneToMany(mappedBy = "courtAppearance", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.EAGER)
  val movements: List<CourtAppearanceMovement>
    field = mutableListOf<CourtAppearanceMovement>()

  fun addMovement(movement: CourtAppearanceMovement) = apply {
    movements.add(movement)
    movement.courtAppearance = this
  }

  fun removeMovement(movement: CourtAppearanceMovement) = apply {
    movements.remove(movement)
    movement.courtAppearance = null
  }

  fun calculateStatus(statusProvider: (CourtAppearanceStatus.Code) -> CourtAppearanceStatus) = apply {
    val statusCode = when {
      isCompleted() -> CourtAppearanceStatus.Code.COMPLETED
      isInProgress() -> CourtAppearanceStatus.Code.IN_PROGRESS
      isExpired() -> CourtAppearanceStatus.Code.EXPIRED
      else -> CourtAppearanceStatus.Code.SCHEDULED
    }
    status = statusProvider(statusCode)
  }

  private fun isCompleted() = movements.any { it.direction == CourtAppearanceMovement.Direction.IN } ||
    (movements.isNotEmpty() && isInThePast())

  private fun isInProgress() = movements.isNotEmpty() && !isInThePast()

  private fun isExpired() = movements.isEmpty() && isInThePast()

  private fun isInThePast() = end?.isBefore(LocalDateTime.now()) ?: start.toLocalDate().isBefore(LocalDate.now())
}
