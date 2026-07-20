package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.AppearanceMovementAppearanceChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.AppearanceMovementCommentsChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.AppearanceMovementDeleted
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.AppearanceMovementMigrated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.AppearanceMovementOccurredAtChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.AppearanceMovementRelocated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceCancelled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceCommentsChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceMigrated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceRelocated
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceRescheduled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceResponsiblePrisonChanged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.externalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations.Companion.unscheduledMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync.SyncGenerator.courtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync.SyncGenerator.courtEventMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonApiMockServer.Companion.prisonerMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerApiExtension.Companion.prisonApi
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.RemandAndSentencingExtension.Companion.rasMockServer
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.schedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEventMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ResyncCourtEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ResyncCourtEventMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ResyncCourtEvents
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ResyncResponse
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal.MigrationSystemAudit
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal.MigrationSystemAuditRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ResyncCourtAppearanceIntTest(
  @Autowired cmo: CourtMovementOperations,
  @Autowired cao: CourtAppearanceOperations,
  @Autowired pso: PersonSummaryOperations,
  @Autowired private val msaRepository: MigrationSystemAuditRepository,
) : IntegrationTest(),
  CourtMovementOperations by cmo,
  CourtAppearanceOperations by cao,
  PersonSummaryOperations by pso {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(URL_TO_TEST, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.SCHEDULER_RO, Roles.SCHEDULER_RW, Roles.SCHEDULER_UI])
  fun `403 forbidden without correct role`(role: String) {
    resync(personIdentifier(), resyncRequest(), role = role)
      .expectStatus().isForbidden
  }

  @Test
  fun `200 ok - can migrate data`() {
    val prisonCode = prisonCode()
    val prisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode))
    prisonApi.givenMovementsFor(prisoner.prisonerNumber, listOf(prisonerMovement(prisonCode)))

    val request = resyncRequest(
      listOf(
        resyncCourtEvent(courtEvent(externalReference = externalReference())),
        resyncCourtEvent(
          movements = listOf(resyncCourtEventMovement()),
          modified = AtAndBy(LocalDateTime.now(), username()),
        ),
      ),
      listOf(resyncCourtEventMovement()),
    )
    rasMockServer.givenReconciliationAppearances(prisoner.prisonerNumber, request.courtEvents.mapNotNull { it.courtEvent.schedule(prisoner.prisonerNumber) })
    val res = resync(prisoner.prisonerNumber, request).successResponse<ResyncResponse>()

    res.courtEvents.forEach { ce ->
      val saved = requireNotNull(findCourtAppearance(ce.dpsId))
      val ceDetail = request.courtEvents.first { it.courtEvent.eventId == ce.eventId }
      saved verifyAgainst ceDetail.courtEvent
      val msa = requireNotNull(msaRepository.findByIdOrNull(saved.id))
      assertThat(msa.createdBy).isEqualTo(ceDetail.created.by)
      ceDetail.modified?.also { assertThat(msa.modifiedBy).isEqualTo(it.by) }
      ce.movements.forEach { mov ->
        val savedMov = requireNotNull(findCourtMovement(mov.dpsId))
        val courtMov = ceDetail.movements.first {
          with(it.movement) { bookingId == mov.bookingId && sequenceNumber == mov.sequenceNumber }
        }
        savedMov verifyAgainst courtMov.movement
      }
      verifyAudit(
        saved,
        RevisionType.ADD,
        setOf(
          HmppsDomainEvent::class.simpleName!!,
          CourtAppearance::class.simpleName!!,
          CourtAppearanceMovement::class.simpleName!!,
        ),
        SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
      )
    }

    val externalRef =
      { legacyId: Long -> request.courtEvents.single { it.courtEvent.eventId == legacyId }.courtEvent.externalReferenceUrn }

    val ca = requireNotNull(findCourtAppearance(res.courtEvents.first().dpsId))
    verifyEventPublications(
      ca,
      (
        res.courtEvents.map {
          CourtAppearanceMigrated(prisoner.prisonerNumber, it.dpsId, externalRef(it.eventId), DataSource.NOMIS)
            .publication(it.dpsId) { false }
        } +
          res.courtEvents.flatMap { ce ->
            ce.movements.map {
              AppearanceMovementMigrated(
                prisoner.prisonerNumber,
                it.dpsId,
                DataSource.NOMIS,
              ).publication(it.dpsId) { false }
            }
          } +
          res.unscheduledMovements.map {
            AppearanceMovementMigrated(
              prisoner.prisonerNumber,
              it.dpsId,
              DataSource.NOMIS,
            ).publication(it.dpsId) { false }
          }
        ).toSet(),
    )
  }

  @Test
  fun `200 ok - can migrate completed appearances with future date`() {
    val prisonCode = prisonCode()
    val prisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode))
    prisonApi.givenMovementsFor(prisoner.prisonerNumber, listOf(prisonerMovement(prisonCode, dateTime = LocalDateTime.now().plusDays(8))))

    val request = resyncRequest(
      listOf(
        resyncCourtEvent(
          courtEvent(
            date = LocalDate.now().plusDays(7),
            externalReference = externalReference(),
          ),
        ),
      ),
    )
    rasMockServer.givenReconciliationAppearances(prisoner.prisonerNumber, request.courtEvents.mapNotNull { it.courtEvent.schedule(prisoner.prisonerNumber) })
    val res = resync(prisoner.prisonerNumber, request).successResponse<ResyncResponse>()

    res.courtEvents.single().also { ce ->
      val saved = requireNotNull(findCourtAppearance(ce.dpsId))
      assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.COMPLETED)
      val ceDetail = request.courtEvents.first { it.courtEvent.eventId == ce.eventId }
      saved verifyAgainst ceDetail.courtEvent
      val msa = requireNotNull(msaRepository.findByIdOrNull(saved.id))
      assertThat(msa.createdBy).isEqualTo(ceDetail.created.by)
      ceDetail.modified?.also { assertThat(msa.modifiedBy).isEqualTo(it.by) }
      verifyAudit(
        saved,
        RevisionType.ADD,
        setOf(
          HmppsDomainEvent::class.simpleName!!,
          CourtAppearance::class.simpleName!!,
        ),
        SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
      )
    }

    val externalRef =
      { legacyId: Long -> request.courtEvents.single { it.courtEvent.eventId == legacyId }.courtEvent.externalReferenceUrn }

    val ca = requireNotNull(findCourtAppearance(res.courtEvents.first().dpsId))
    verifyEventPublications(
      ca,
      res.courtEvents.map {
        CourtAppearanceMigrated(prisoner.prisonerNumber, it.dpsId, externalRef(it.eventId), DataSource.NOMIS)
          .publication(it.dpsId) { false }
      }.toSet(),
    )
  }

  @Test
  fun `200 ok - can migrate unscheduled appearance`() {
    val prisonCode = prisonCode()
    val prisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode))
    prisonApi.givenMovementsFor(prisoner.prisonerNumber, listOf(prisonerMovement(prisonCode)))

    val request = resyncRequest(
      listOf(
        resyncCourtEvent(
          courtEvent(
            date = LocalDate.now().plusDays(7),
            externalReference = externalReference(),
            status = "SCHED",
            currentTerm = false,
          ),
        ),
      ),
    )
    rasMockServer.givenReconciliationAppearances(prisoner.prisonerNumber, request.courtEvents.mapNotNull { it.courtEvent.schedule(prisoner.prisonerNumber) })
    val res = resync(prisoner.prisonerNumber, request).successResponse<ResyncResponse>()

    res.courtEvents.single().also { ce ->
      val saved = requireNotNull(findCourtAppearance(ce.dpsId))
      assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.UNSCHEDULED)
      val ceDetail = request.courtEvents.first { it.courtEvent.eventId == ce.eventId }
      saved verifyAgainst ceDetail.courtEvent
      val msa = requireNotNull(msaRepository.findByIdOrNull(saved.id))
      assertThat(msa.createdBy).isEqualTo(ceDetail.created.by)
      ceDetail.modified?.also { assertThat(msa.modifiedBy).isEqualTo(it.by) }
      verifyAudit(
        saved,
        RevisionType.ADD,
        setOf(
          HmppsDomainEvent::class.simpleName!!,
          CourtAppearance::class.simpleName!!,
        ),
        SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
      )
    }

    val externalRef =
      { legacyId: Long -> request.courtEvents.single { it.courtEvent.eventId == legacyId }.courtEvent.externalReferenceUrn }

    val ca = requireNotNull(findCourtAppearance(res.courtEvents.first().dpsId))
    verifyEventPublications(
      ca,
      res.courtEvents.map {
        CourtAppearanceMigrated(prisoner.prisonerNumber, it.dpsId, externalRef(it.eventId), DataSource.NOMIS)
          .publication(it.dpsId) { false }
      }.toSet(),
    )
  }

  @Test
  fun `200 ok - can migrate completed appearance without IN movement`() {
    val prisonCode = prisonCode()
    val person = givenPersonSummary(personSummary(prisonCode = prisonCode))
    prisonApi.givenMovementsFor(person.identifier, listOf(prisonerMovement(prisonCode, dateTime = LocalDateTime.now())))

    val request = resyncRequest(
      listOf(
        resyncCourtEvent(
          courtEvent(externalReference = externalReference(), date = LocalDate.now().minusDays(1)),
          movements = listOf(resyncCourtEventMovement()),
        ),
      ),
    )
    rasMockServer.givenReconciliationAppearances(person.identifier, request.courtEvents.mapNotNull { it.courtEvent.schedule(person.identifier) })
    val res = resync(person.identifier, request).successResponse<ResyncResponse>()

    res.courtEvents.single().also { ce ->
      val saved = requireNotNull(findCourtAppearance(ce.dpsId))
      assertThat(saved.status.code).isEqualTo(CourtAppearanceStatus.Code.COMPLETED)
      val ceDetail = request.courtEvents.first { it.courtEvent.eventId == ce.eventId }
      saved verifyAgainst ceDetail.courtEvent
      verifyAudit(
        saved,
        RevisionType.ADD,
        setOf(
          HmppsDomainEvent::class.simpleName!!,
          CourtAppearance::class.simpleName!!,
          CourtAppearanceMovement::class.simpleName!!,
        ),
        SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
      )
    }

    val externalRef =
      { legacyId: Long -> request.courtEvents.single { it.courtEvent.eventId == legacyId }.courtEvent.externalReferenceUrn }

    val ca = requireNotNull(findCourtAppearance(res.courtEvents.first().dpsId))
    verifyEventPublications(
      ca,
      (
        res.courtEvents.map {
          CourtAppearanceMigrated(person.identifier, it.dpsId, externalRef(it.eventId), DataSource.NOMIS)
            .publication(it.dpsId) { false }
        } +
          res.courtEvents.flatMap { ce ->
            ce.movements.map {
              AppearanceMovementMigrated(
                person.identifier,
                it.dpsId,
                DataSource.NOMIS,
              ).publication(it.dpsId) { false }
            }
          }
        ).toSet(),
    )
  }

  @Test
  fun `200 ok - can remove all data`() {
    val schedule = givenCourtAppearance(courtAppearance(movements = listOf(movement(CourtAppearanceMovement.Direction.OUT))))
    val scheduled = schedule.movements.single()
    val unscheduled = givenUnscheduledMovement(unscheduledMovement(schedule.person.identifier, schedule.prisonCode))
    rasMockServer.givenReconciliationAppearances(schedule.person.identifier, emptyList())
    prisonApi.givenMovementsFor(schedule.person.identifier, listOf(prisonerMovement(schedule.prisonCode)))

    val res = resync(schedule.person.identifier, resyncRequest()).successResponse<ResyncResponse>()
    assertThat(res.courtEvents).isEmpty()
    assertThat(res.unscheduledMovements).isEmpty()

    assertThat(findCourtAppearance(schedule.id)).isNull()
    assertThat(findCourtMovement(unscheduled.id)).isNull()
    assertThat(findPersonSummary(schedule.person.identifier)).isNull()

    verifyAudit(
      schedule,
      RevisionType.DEL,
      setOf(
        CourtAppearance::class.simpleName!!,
        CourtAppearanceMovement::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      schedule,
      setOf(
        CourtAppearanceCancelled(
          schedule.person.identifier,
          schedule.id,
          schedule.externalReference,
          DataSource.NOMIS,
        ).publication(schedule.id) { false },
        AppearanceMovementDeleted(
          scheduled.person.identifier,
          scheduled.id,
          DataSource.NOMIS,
        ).publication(scheduled.id) { false },
        AppearanceMovementDeleted(
          unscheduled.person.identifier,
          unscheduled.id,
          DataSource.NOMIS,
        ).publication(unscheduled.id) { false },
      ),
    )
  }

  @Test
  fun `200 ok - can merge, updating records`() {
    val prevPrisonCode = prisonCode()
    val prisonCode = prisonCode()
    val person = givenPersonSummary(personSummary(prisonCode = prisonCode))
    val externalReference = externalReference()
    val schedule = givenCourtAppearance(
      courtAppearance(
        personIdentifier = person.identifier,
        prisonCode = prevPrisonCode,
        externalReference = externalReference,
        movements = listOf(movement(CourtAppearanceMovement.Direction.OUT)),
      ),
    )
    val scheduled = schedule.movements.single()
    val unscheduled = givenUnscheduledMovement(unscheduledMovement(person.identifier, prisonCode))
    prisonApi.givenMovementsFor(person.identifier, listOf(prisonerMovement(prevPrisonCode), prisonerMovement(prisonCode)))

    val request = resyncRequest(
      courtEvents = listOf(
        resyncCourtEvent(
          courtEvent = courtEvent(dpsId = schedule.id, externalReference = externalReference),
          movements = listOf(resyncCourtEventMovement(movement = courtEventMovement(dpsId = scheduled.id))),
          modified = AtAndBy(LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS), username()),
        ),
      ),
      unscheduledMovements = listOf(resyncCourtEventMovement(movement = courtEventMovement(dpsId = unscheduled.id))),
    )
    val courtEvent = request.courtEvents.first()
    val originalMsa = MigrationSystemAudit(schedule.id, courtEvent.created.at, courtEvent.created.by, null, null)
    msaRepository.save(originalMsa)
    rasMockServer.givenReconciliationAppearances(person.identifier, request.courtEvents.mapNotNull { it.courtEvent.schedule(person.identifier) })

    val res = resync(person.identifier, request).successResponse<ResyncResponse>()
    assertThat(res.courtEvents.first().dpsId).isEqualTo(schedule.id)
    assertThat(res.courtEvents.first().movements.first().dpsId).isEqualTo(scheduled.id)
    assertThat(res.unscheduledMovements.first().dpsId).isEqualTo(unscheduled.id)
    val updatedMsa = requireNotNull(msaRepository.findByIdOrNull(schedule.id))
    assertThat(updatedMsa.modifiedBy).isEqualTo(courtEvent.modified!!.by)
    assertThat(updatedMsa.modifiedAt).isCloseTo(courtEvent.modified.at, within(1, ChronoUnit.SECONDS))

    verifyAudit(
      scheduled,
      RevisionType.MOD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        CourtAppearance::class.simpleName!!,
        CourtAppearanceMovement::class.simpleName!!,
      ),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      scheduled,
      setOf(
        CourtAppearanceResponsiblePrisonChanged(
          person.identifier,
          schedule.id,
          externalReference,
          DataSource.NOMIS,
        ).publication(schedule.id) { false },
        CourtAppearanceRescheduled(
          person.identifier,
          schedule.id,
          externalReference,
          DataSource.NOMIS,
        ).publication(schedule.id) { false },
        CourtAppearanceRelocated(
          person.identifier,
          schedule.id,
          externalReference,
          DataSource.NOMIS,
        ).publication(schedule.id) { false },
        CourtAppearanceCommentsChanged(
          person.identifier,
          schedule.id,
          externalReference,
          DataSource.NOMIS,
        ).publication(schedule.id) { false },
        AppearanceMovementRelocated(
          person.identifier,
          scheduled.id,
          DataSource.NOMIS,
        ).publication(scheduled.id) { false },
        AppearanceMovementRelocated(
          person.identifier,
          unscheduled.id,
          DataSource.NOMIS,
        ).publication(unscheduled.id) { false },
        AppearanceMovementCommentsChanged(
          person.identifier,
          scheduled.id,
          DataSource.NOMIS,
        ).publication(scheduled.id) { false },
        AppearanceMovementCommentsChanged(
          person.identifier,
          unscheduled.id,
          DataSource.NOMIS,
        ).publication(unscheduled.id) { false },
        AppearanceMovementOccurredAtChanged(
          person.identifier,
          scheduled.id,
          DataSource.NOMIS,
        ).publication(scheduled.id) { false },
        AppearanceMovementOccurredAtChanged(
          person.identifier,
          unscheduled.id,
          DataSource.NOMIS,
        ).publication(unscheduled.id) { false },
      ),
    )
  }

  @Test
  fun `200 ok - can reverse scheduled and unscheduled movements`() {
    val prisonCode = prisonCode()
    val courtCode = courtCode()
    val person = givenPersonSummary(personSummary(prisonCode = prisonCode))
    val bookingId = newId()
    val schedule = givenCourtAppearance(
      courtAppearance(
        personIdentifier = person.identifier,
        prisonCode = prisonCode,
        courtCode = courtCode,
        movements = listOf(movement(CourtAppearanceMovement.Direction.OUT, legacyId = "${bookingId}_1")),
        legacyId = newId(),
      ),
    )
    val scheduled = schedule.movements.first()
    val unscheduled =
      givenUnscheduledMovement(
        unscheduledMovement(
          person.identifier,
          prisonCode,
          legacyId = "${bookingId}_2",
          courtCode = courtCode,
        ),
      )
    rasMockServer.givenReconciliationAppearances(person.identifier, emptyList())
    prisonApi.givenMovementsFor(person.identifier, listOf(prisonerMovement(prisonCode)))

    val request = resyncRequest(
      courtEvents = listOf(
        resyncCourtEvent(
          courtEvent = courtEvent(
            dpsId = schedule.id,
            eventId = schedule.legacyId!!,
            scheduledPrisonCode = prisonCode,
            scheduledCourtCode = courtCode,
            date = schedule.start.toLocalDate(),
            startTime = schedule.start.toLocalTime(),
            commentText = schedule.comments,
          ),
          movements = listOf(
            resyncCourtEventMovement(
              movement = courtEventMovement(
                dpsId = unscheduled.id,
                bookingId = unscheduled.bookingId!!,
                sequenceNumber = unscheduled.sequenceNumber!!,
                fromAgencyId = prisonCode,
                toAgencyId = unscheduled.courtCode,
                date = unscheduled.occurredAt.toLocalDate(),
                time = unscheduled.occurredAt.toLocalTime(),
                commentText = unscheduled.comments,
              ),
            ),
          ),
        ),
      ),
      unscheduledMovements = listOf(
        resyncCourtEventMovement(
          movement = courtEventMovement(
            dpsId = scheduled.id,
            bookingId = scheduled.bookingId!!,
            sequenceNumber = scheduled.sequenceNumber!!,
            fromAgencyId = prisonCode,
            toAgencyId = scheduled.courtCode,
            date = scheduled.occurredAt.toLocalDate(),
            time = scheduled.occurredAt.toLocalTime(),
            commentText = scheduled.comments,
          ),
        ),
      ),
    )
    val res = resync(person.identifier, request).successResponse<ResyncResponse>()
    assertThat(res.courtEvents.first().movements.first().dpsId).isEqualTo(unscheduled.id)
    assertThat(res.unscheduledMovements.first().dpsId).isEqualTo(scheduled.id)

    verifyAudit(
      scheduled,
      RevisionType.MOD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        CourtAppearance::class.simpleName!!,
        CourtAppearanceMovement::class.simpleName!!,
      ),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      scheduled,
      setOf(
        AppearanceMovementAppearanceChanged(
          scheduled.person.identifier,
          scheduled.id,
          DataSource.NOMIS,
        ).publication(scheduled.id) { false },
        AppearanceMovementAppearanceChanged(
          unscheduled.person.identifier,
          unscheduled.id,
          DataSource.NOMIS,
        ).publication(unscheduled.id) { false },
      ),
    )
  }

  @Test
  fun `200 ok - can switch external reference`() {
    val prisonCode = prisonCode()
    val courtCode = courtCode()
    val person = givenPersonSummary(personSummary(prisonCode = prisonCode))
    val existing = givenCourtAppearance(
      courtAppearance(
        personIdentifier = person.identifier,
        prisonCode = prisonCode,
        courtCode = courtCode,
        legacyId = newId(),
        externalReference = externalReference(),
      ),
    )

    val request = resyncRequest(
      courtEvents = listOf(
        resyncCourtEvent(
          courtEvent = courtEvent(
            dpsId = existing.id,
            eventId = existing.legacyId!!,
            scheduledPrisonCode = prisonCode,
            scheduledCourtCode = courtCode,
            date = existing.start.toLocalDate(),
            startTime = existing.start.toLocalTime(),
            commentText = existing.comments,
            externalReference = externalReference(),
          ),
        ),
        resyncCourtEvent(
          courtEvent = courtEvent(
            scheduledPrisonCode = prisonCode,
            scheduledCourtCode = courtCode,
            date = existing.start.toLocalDate(),
            startTime = existing.start.toLocalTime(),
            commentText = existing.comments,
            externalReference = existing.externalReference,
          ),
        ),
      ),
    )
    rasMockServer.givenReconciliationAppearances(person.identifier, request.courtEvents.mapNotNull { it.courtEvent.schedule(person.identifier) })
    prisonApi.givenMovementsFor(person.identifier, listOf(prisonerMovement(prisonCode)))
    val res = resync(person.identifier, request).successResponse<ResyncResponse>()
    assertThat(res.courtEvents.size).isEqualTo(2)

    val updated = requireNotNull(findCourtAppearance(existing.id))
    updated verifyAgainst request.courtEvents.first { rq -> rq.courtEvent.eventId == updated.legacyId }.courtEvent
    assertThat(updated.externalReference).isNotEqualTo(existing.externalReference)
    verifyAudit(
      updated,
      RevisionType.MOD,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
    )

    val newId = res.courtEvents.first { it.dpsId != existing.id }.dpsId
    val newAppearance = requireNotNull(findCourtAppearance(newId))
    newAppearance verifyAgainst request.courtEvents.first { rq -> rq.courtEvent.eventId == newAppearance.legacyId }.courtEvent
    assertThat(newAppearance.externalReference).isEqualTo(existing.externalReference)
    verifyAudit(
      newAppearance,
      RevisionType.ADD,
      setOf(CourtAppearance::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      newAppearance,
      setOf(
        CourtAppearanceMigrated(
          newAppearance.person.identifier,
          newAppearance.id,
          newAppearance.externalReference,
          DataSource.NOMIS,
        ).publication(newAppearance.id) { false },
      ),
    )
  }

  private val CourtAppearanceMovement.bookingId get() = legacyId?.split('_')[0]?.toLong()
  private val CourtAppearanceMovement.sequenceNumber get() = legacyId?.split('_')[1]?.toInt()

  private fun resyncRequest(
    courtEvents: List<ResyncCourtEvent> = listOf(),
    unscheduledMovements: List<ResyncCourtEventMovement> = listOf(),
  ) = ResyncCourtEvents(courtEvents, unscheduledMovements)

  private fun resyncCourtEvent(
    courtEvent: CourtEvent = courtEvent(),
    created: AtAndBy = AtAndBy(LocalDateTime.now().minusDays(2).truncatedTo(ChronoUnit.SECONDS), username()),
    modified: AtAndBy? = null,
    movements: List<ResyncCourtEventMovement> = listOf(),
  ) = ResyncCourtEvent(courtEvent, created, modified, movements)

  private fun resyncCourtEventMovement(
    movement: CourtEventMovement = courtEventMovement(),
    created: AtAndBy = AtAndBy(LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS), username()),
    modified: AtAndBy? = null,
  ) = ResyncCourtEventMovement(movement, created, modified)

  private fun resync(
    personIdentifier: String,
    request: ResyncCourtEvents,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(URL_TO_TEST, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = "NOMIS_SYNC", roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/resync/court-appearances/{personIdentifier}"
  }
}
