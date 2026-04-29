package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations.Companion.unscheduledMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.ReconciliationResponse

class ReconciliationRetrieverIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired cam: CourtMovementOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao,
  CourtMovementOperations by cam {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(URL_TO_TEST, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.SCHEDULER_UI, Roles.SCHEDULER_RW, Roles.SCHEDULER_RO])
  fun `403 forbidden without correct role`(role: String) {
    getReconciliationDetails(personIdentifier(), role).expectStatus().isForbidden
  }

  @Test
  fun `200 ok when no data`() {
    val personIdentifier = personIdentifier()
    val res = getReconciliationDetails(personIdentifier).successResponse<ReconciliationResponse>()
    assertThat(res.courtEvents).isEmpty()
    assertThat(res.unscheduledMovements).isEmpty()
  }

  @Test
  fun `200 can retrieve all scheduled and unscheduled for a person`() {
    val personIdentifier = personIdentifier()
    val scheduled = givenCourtAppearance(
      courtAppearance(
        personIdentifier,
        movements = listOf(movement(CourtAppearanceMovement.Direction.OUT)),
      ),
    )
    val unscheduled = givenUnscheduledMovement(unscheduledMovement(personIdentifier))

    val res = getReconciliationDetails(personIdentifier).successResponse<ReconciliationResponse>()
    with(res.courtEvents.first()) {
      scheduled verifyAgainst courtEvent
      scheduled.movements.single() verifyAgainst movements.single()
    }
    with(res.unscheduledMovements) {
      unscheduled verifyAgainst single()
    }
  }

  @Test
  fun `200 can retrieve schedule without movements`() {
    val personIdentifier = personIdentifier()
    val scheduled = givenCourtAppearance(courtAppearance(personIdentifier))

    val res = getReconciliationDetails(personIdentifier).successResponse<ReconciliationResponse>()
    scheduled verifyAgainst res.courtEvents.first().courtEvent
  }

  private fun getReconciliationDetails(
    personIdentifier: String,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .get()
    .uri(URL_TO_TEST, personIdentifier)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/reconciliation/court-appearances/{personIdentifier}"
  }
}
