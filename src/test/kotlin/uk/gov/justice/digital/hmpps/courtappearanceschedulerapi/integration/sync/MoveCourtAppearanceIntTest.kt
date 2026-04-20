package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations.Companion.unscheduledMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.MoveCourtEventRequest
import java.util.UUID

class MoveCourtAppearanceIntTest(
  @Autowired cmo: CourtMovementOperations,
  @Autowired cao: CourtAppearanceOperations,
  @Autowired pso: PersonSummaryOperations,
) : IntegrationTest(),
  CourtMovementOperations by cmo,
  CourtAppearanceOperations by cao,
  PersonSummaryOperations by pso {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(URL_TO_TEST)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.SCHEDULER_RO, Roles.SCHEDULER_RW, Roles.SCHEDULER_UI])
  fun `403 forbidden without correct role`(role: String) {
    moveAppearance(moveRequest(), role = role).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request if id not linked to from person identifier`() {
    val prisonCode = prisonCode()
    val moveTo = prisonerSearch.givenPrisoner(prisoner(prisonCode))
    val appearance = givenCourtAppearance(courtAppearance())
    moveAppearance(moveRequest(personIdentifier(), moveTo.prisonerNumber, setOf(appearance.id)))
      .expectStatus().isBadRequest

    val unchanged = requireNotNull(findCourtAppearance(appearance.id))
    assertThat(unchanged.person.identifier).isEqualTo(appearance.person.identifier)
  }

  @Test
  fun `200 ok can move selected appearances and movements to another person identifier`() {
    val prisonCode = prisonCode()
    val p1 = givenPersonSummary(personSummary(prisonCode = prisonCode))
    val p2 = givenPersonSummary(personSummary(prisonCode = prisonCode))

    val p1Sch1 = givenCourtAppearance(
      courtAppearance(
        p1.identifier,
        prisonCode,
        movements = listOf(
          movement(
            CourtAppearanceMovement.Direction.OUT,
          ),
        ),
      ),
    )
    val p1Sch1Movement = p1Sch1.movements.first()
    val p1Sch2 = givenCourtAppearance(courtAppearance(p1.identifier, prisonCode))
    val p1UnS1 = givenUnscheduledMovement(unscheduledMovement(p1.identifier, prisonCode))
    val p2Sch1 = givenCourtAppearance(courtAppearance(p2.identifier, prisonCode))

    moveAppearance(moveRequest(p1.identifier, p2.identifier, setOf(p1Sch1.id), setOf(p1UnS1.id)))

    with(requireNotNull(findCourtAppearance(p1Sch2.id))) {
      assertThat(person.identifier).isEqualTo(p1.identifier)
    }

    with(requireNotNull(findCourtAppearance(p2Sch1.id))) {
      assertThat(person.identifier).isEqualTo(p2.identifier)
    }

    val context = SchedulerContext.get().copy(reason = "Prisoner booking moved", source = DataSource.NOMIS)
    with(requireNotNull(findCourtAppearance(p1Sch1.id))) {
      assertThat(person.identifier).isEqualTo(p2.identifier)
      verifyAudit(
        this,
        RevisionType.MOD,
        setOf(CourtAppearance::class.simpleName!!, CourtAppearanceMovement::class.simpleName!!),
        context,
      )
    }
    with(requireNotNull(findCourtMovement(p1Sch1Movement.id))) {
      assertThat(person.identifier).isEqualTo(p2.identifier)
      verifyAudit(
        this,
        RevisionType.MOD,
        setOf(CourtAppearance::class.simpleName!!, CourtAppearanceMovement::class.simpleName!!),
        context,
      )
    }
    with(requireNotNull(findCourtMovement(p1UnS1.id))) {
      assertThat(person.identifier).isEqualTo(p2.identifier)
      verifyAudit(
        this,
        RevisionType.MOD,
        setOf(CourtAppearance::class.simpleName!!, CourtAppearanceMovement::class.simpleName!!),
        context,
      )
    }
  }

  @Test
  fun `200 ok orphaned person summary removed`() {
    val prisonCode = prisonCode()
    val p1 = givenPersonSummary(personSummary(prisonCode = prisonCode))
    val p2 = prisonerSearch.givenPrisoner(prisoner(prisonCode))

    val p1Sch = givenCourtAppearance(courtAppearance(p1.identifier, prisonCode))
    val p1Uns = givenUnscheduledMovement(unscheduledMovement(p1.identifier, prisonCode))
    val p2Sch = givenCourtAppearance(courtAppearance(p2.prisonerNumber, prisonCode))

    moveAppearance(moveRequest(p1.identifier, p2.prisonerNumber, setOf(p1Sch.id), setOf(p1Uns.id)))

    with(requireNotNull(findCourtAppearance(p2Sch.id))) {
      assertThat(person.identifier).isEqualTo(p2.prisonerNumber)
    }

    val context = SchedulerContext.get().copy(reason = "Prisoner booking moved", source = DataSource.NOMIS)
    with(requireNotNull(findCourtAppearance(p1Sch.id))) {
      assertThat(person.identifier).isEqualTo(p2.prisonerNumber)
      verifyAudit(
        this,
        RevisionType.MOD,
        setOf(CourtAppearance::class.simpleName!!, CourtAppearanceMovement::class.simpleName!!),
        context,
      )
    }
    with(requireNotNull(findCourtMovement(p1Uns.id))) {
      assertThat(person.identifier).isEqualTo(p2.prisonerNumber)
      verifyAudit(
        this,
        RevisionType.MOD,
        setOf(CourtAppearance::class.simpleName!!, CourtAppearanceMovement::class.simpleName!!),
        context,
      )
    }

    assertThat(findPersonSummary(p1.identifier)).isNull()
  }

  private fun moveRequest(
    from: String = personIdentifier(),
    to: String = personIdentifier(),
    scheduleIds: Set<UUID> = setOf(),
    unscheduledMovementIds: Set<UUID> = setOf(),
  ) = MoveCourtEventRequest(from, to, scheduleIds, unscheduledMovementIds)

  private fun moveAppearance(
    request: MoveCourtEventRequest,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(URL_TO_TEST)
    .bodyValue(request)
    .headers(setAuthorisation(username = "NOMIS_SYNC", roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/move/court-appearances"
  }
}
