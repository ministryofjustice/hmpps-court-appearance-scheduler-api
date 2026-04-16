package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.AppearanceMovementMigrated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.AppearanceMovementRecorded
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement.AppearanceMovementAction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement.ChangeMovementComments
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement.RecategoriseMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement.RelocateMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.movement.changes
import java.time.LocalDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = "court_appearance_movement")
final class CourtAppearanceMovement(
  courtAppearance: CourtAppearance?,
  person: PersonSummary,
  prisonCode: String,
  courtCode: String,
  reason: CourtAppearanceReason,
  direction: Direction,
  occurredAt: LocalDateTime,
  comments: String?,
  legacyId: String?,
  @Id
  @Column(name = "id", nullable = false)
  override val id: UUID = newUuid(),
) : DomainEventProducer {
  @Version
  @Column(name = "version", nullable = false)
  override var version: Int? = null
    private set

  @ManyToOne(optional = true)
  @JoinColumn(name = "court_appearance_id", nullable = true)
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

  @Fetch(FetchMode.JOIN)
  @Audited(targetAuditMode = NOT_AUDITED)
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "reason_id", nullable = false)
  var reason: CourtAppearanceReason = reason
    private set

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

  @Transient
  private var appliedActions: List<AppearanceMovementAction> = listOf()

  @PostLoad
  private fun load() {
    appliedActions = listOf()
  }

  override fun initialEvents(): Set<DomainEventPublication> = if (SchedulerContext.get().migratingData) {
    setOf(AppearanceMovementMigrated(person.identifier, id).publication(id) { false })
  } else {
    setOf(
      AppearanceMovementRecorded(person.identifier, id).publication(id),
    )
  }

  override fun domainEvents(): Set<DomainEventPublication> = appliedActions.mapNotNull { action ->
    action.domainEvent(this)?.publication(id)
  }.toSet()

  fun movePerson(person: PersonSummary) {
    this.person = person
  }

  fun applyLegacyId(legacyId: String) {
    this.legacyId = legacyId
  }

  fun moveSchedule(appearance: CourtAppearance?, statusProvider: StatusProvider) {
    if (this.courtAppearance?.id != appearance?.id) {
      courtAppearance?.removeMovement(this)?.calculateStatus(statusProvider)
      appearance?.addMovement(this)?.calculateStatus(statusProvider)
    }
  }

  fun recategorise(action: RecategoriseMovement, reasonProvider: ReasonProvider) = apply {
    if (action.reasonCode != reason.code) {
      reason = reasonProvider(action.reasonCode)
      appliedActions += action
    }
  }

  fun relocate(action: RelocateMovement) = apply {
    if (action.courtCode != courtCode) {
      courtCode = action.courtCode
      appliedActions += action
    }
  }

  fun applyComments(action: ChangeMovementComments) = apply {
    if (action changes ::comments) {
      appliedActions += action
    }
  }
}

interface CourtAppearanceMovementRepository : JpaRepository<CourtAppearanceMovement, UUID> {
  fun findByLegacyId(legacyId: String): CourtAppearanceMovement?
  fun countByPersonIdentifier(personIdentifier: String): Int

  @Query(
    """
    select cam.id from CourtAppearanceMovement cam
    where cam.person.identifier = :personIdentifier
  """,
  )
  fun findIdsForPersonIdentifier(personIdentifier: String): List<UUID>

  @Query(
    """
    select cam.id from CourtAppearanceMovement cam
    where cam.legacyId in :legacyIds
  """,
  )
  fun findIdsForLegacyIds(legacyIds: Set<String>): List<UUID>

  fun findAllByPersonIdentifier(personIdentifier: String): List<CourtAppearanceMovement>
}
