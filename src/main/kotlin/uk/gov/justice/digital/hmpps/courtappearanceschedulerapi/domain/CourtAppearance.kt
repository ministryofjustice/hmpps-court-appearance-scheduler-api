package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
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
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement.Direction.IN
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement.Direction.OUT
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceCancelled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceMigrated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceRecorded
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceScheduled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ExternalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ChangeAppearanceComments
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ChangeAppearancePrison
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.CompleteAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.CourtAppearanceAction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ExpireAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RecategoriseAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RelocateAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RequestAppearanceByVideoLink
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RequestAppearanceInPerson
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RescheduleAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ScheduleAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.StartAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.UnscheduleAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.changes
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
  externalReference: ExternalReference?,
  legacyId: Long?,
  @Id
  @Column(name = "id", nullable = false)
  override val id: UUID = newUuid(),
) : Identifiable,
  DomainEventProducer {
  @Version
  @Column(name = "version", nullable = false)
  override var version: Int? = null
    private set

  @Fetch(FetchMode.JOIN)
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
    private set(value) {
      field = value
      external = value.external
    }

  @Fetch(FetchMode.JOIN)
  @Audited(targetAuditMode = NOT_AUDITED)
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  lateinit var status: CourtAppearanceStatus
    private set

  @NotNull
  @Column(name = "external", nullable = false)
  var external: Boolean = reason.external
    private set(value) {
      if (field != value) {
        appliedActions += if (value) RequestAppearanceInPerson() else RequestAppearanceByVideoLink()
        field = value
      }
    }

  @NotNull
  @Column(name = "start", nullable = false)
  var start: LocalDateTime = start
    private set

  @Column(name = "end")
  var end: LocalDateTime? = end
    private set

  @Column(name = "comments", length = Integer.MAX_VALUE)
  var comments: String? = comments?.trim()
    private set

  @Convert(converter = ExternalReferenceConverter::class)
  @Column(name = "external_reference")
  var externalReference: ExternalReference? = externalReference
    private set

  @Column(name = "legacy_id")
  var legacyId: Long? = legacyId
    private set

  @Fetch(FetchMode.JOIN)
  @OneToMany(mappedBy = "courtAppearance", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.EAGER)
  val movements: List<CourtAppearanceMovement>
    field = mutableListOf<CourtAppearanceMovement>()

  @Transient
  private var appliedActions: List<CourtAppearanceAction> = listOf()

  @PostLoad
  private fun load() {
    appliedActions = listOf()
  }

  override fun initialEvents(): Set<DomainEventPublication> = if (SchedulerContext.get().migratingData) {
    setOf(CourtAppearanceMigrated(person.identifier, id, externalReference).publication(id) { false })
  } else {
    when (status.code) {
      CourtAppearanceStatus.Code.SCHEDULED -> setOf(
        CourtAppearanceScheduled(person.identifier, id, externalReference).publication(id),
      )

      else -> setOf(CourtAppearanceRecorded(person.identifier, id, externalReference).publication(id))
    }
  }

  override fun domainEvents(): Set<DomainEventPublication> = appliedActions.mapNotNull { action ->
    action.domainEvent(this)?.publication(id)
  }.toSet()

  override fun deletionEvents(): Set<DomainEventPublication> = setOf(
    CourtAppearanceCancelled(person.identifier, id, externalReference).publication(id),
  )

  fun addMovement(movement: CourtAppearanceMovement) = apply {
    val action = when (movement.direction) {
      OUT if (movements.isEmpty()) -> StartAppearance()
      IN if (movements.none { it.direction == IN }) -> CompleteAppearance()
      else -> null
    }
    movements.add(movement)
    action?.also { appliedActions += it }
    movement.courtAppearance = this
  }

  fun removeMovement(movement: CourtAppearanceMovement) = apply {
    movements.remove(movement)
    movement.courtAppearance = null
  }

  fun movePerson(person: PersonSummary) = apply {
    this.person = person
  }

  fun applyResponsibility(action: ChangeAppearancePrison) = apply {
    if (action.prisonCode != prisonCode) {
      prisonCode = action.prisonCode
      appliedActions += action
    }
  }

  fun calculateStatus(
    statusProvider: StatusProvider,
    completeOverride: Boolean = false,
    unscheduleOverride: Boolean = false,
  ) = apply {
    val (statusCode, action) = when {
      unscheduleOverride -> CourtAppearanceStatus.Code.UNSCHEDULED to UnscheduleAppearance()
      completeOverride || isCompleted() -> CourtAppearanceStatus.Code.COMPLETED to CompleteAppearance()
      isInProgress() -> CourtAppearanceStatus.Code.IN_PROGRESS to StartAppearance()
      isExpired() -> CourtAppearanceStatus.Code.EXPIRED to ExpireAppearance()
      else -> CourtAppearanceStatus.Code.SCHEDULED to ScheduleAppearance()
    }
    if (::status.isInitialized.not() || status.code != statusCode) {
      status = statusProvider(statusCode)
      appliedActions += action
    }
  }

  private fun isCompleted() = movements.any { it.direction == IN } ||
    (movements.isNotEmpty() && isInThePast())

  private fun isInProgress() = movements.isNotEmpty() && !isInThePast()

  private fun isExpired() = movements.isEmpty() && isInThePast()

  private fun isInThePast() = end?.isBefore(LocalDateTime.now()) ?: start.toLocalDate().isBefore(LocalDate.now())

  fun recategorise(action: RecategoriseAppearance, reasonProvider: ReasonProvider) = apply {
    if (action.reasonCode != reason.code) {
      reason = reasonProvider(action.reasonCode)
      appliedActions += action
    }
  }

  fun relocate(action: RelocateAppearance) = apply {
    if (action.courtCode != courtCode) {
      courtCode = action.courtCode
      appliedActions += action
    }
  }

  fun reschedule(action: RescheduleAppearance) = apply {
    if (action.changes(::start, ::end)) {
      appliedActions += action
    }
  }

  fun applyComments(action: ChangeAppearanceComments) = apply {
    if (action changes comments) {
      comments = action.comments
      appliedActions += action
    }
  }

  fun applyExternalIdentifiers(externalReference: ExternalReference?, legacyId: Long?) {
    this.externalReference = externalReference
    this.legacyId = legacyId
  }

  companion object {
    fun changeableProperties() = listOf(
      CourtAppearance::prisonCode,
      CourtAppearance::courtCode,
      CourtAppearance::comments,
      CourtAppearance::start,
      CourtAppearance::end,
      CourtAppearance::reason,
    )
  }
}
