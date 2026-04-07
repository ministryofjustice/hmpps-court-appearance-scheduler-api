package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import java.time.LocalDateTime
import java.util.*

@Audited
@Entity
@Table(name = "court_appearance_movement")
final class CourtAppearanceMovement(
  courtAppearance: CourtAppearance?,
  person: PersonSummary,
  prisonCode: String,
  courtCode: String,
  direction: Direction,
  occurredAt: LocalDateTime,
  comments: String?,
  legacyId: String?,
  @Id
  @Column(name = "id", nullable = false)
  val id: UUID = newUuid(),
) {
  @Version
  @Column(name = "version", nullable = false)
  var version: Int? = null
    private set

  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "court_appearance_id", nullable = false)
  var courtAppearance: CourtAppearance? = courtAppearance
    internal set

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

  @Size(max = 3)
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "direction", nullable = false, length = 3)
  var direction: Direction = direction
    private set

  @NotNull
  @Column(name = "occurred_at", nullable = false)
  var occurredAt: LocalDateTime = occurredAt
    private set

  @Column(name = "comments")
  var comments: String? = comments
    private set

  @Size(max = 32)
  @Column(name = "legacy_id", length = 32)
  var legacyId: String? = legacyId
    private set

  enum class Direction {
    IN,
    OUT,
  }
}
