package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations.Companion.unscheduledMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationResponse
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationResponses
import java.time.temporal.ChronoUnit
import java.util.UUID

class IntegrationEndpointsIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired cam: CourtMovementOperations,
) : IntegrationTest(),
  CourtAppearanceOperations by cao,
  CourtMovementOperations by cam {

  @ParameterizedTest
  @MethodSource("allUrls")
  fun `401 unauthorised without a valid token`(url: String) {
    webTestClient
      .get()
      .uri(url, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @MethodSource("allUrls")
  fun `403 forbidden without valid role`(url: String) {
    getIntegrationData(url, role = listOf(Roles.SCHEDULER_UI, Roles.NOMIS_SYNC).random())
  }

  @Test
  fun `can retrieve an appearance`() {
    val app = givenCourtAppearance(courtAppearance())
    val res = getIntegrationData(APPEARANCE_URL, app.id).successResponse<IntegrationResponse<IntegrationAppearance>>()
    res.data verifyAgainst app
    assertThat(res.previousUrl).isNull()
    assertThat(res.nextUrl).isEqualTo(BASE_TEST_URL + (APPEARANCE_MOVEMENTS_URL from app.id))
  }

  @Test
  fun `can retrieve appearance movements`() {
    val app = givenCourtAppearance(
      courtAppearance(
        movements = listOf(
          movement(CourtAppearanceMovement.Direction.OUT),
          movement(CourtAppearanceMovement.Direction.IN),
        ),
      ),
    )
    val mov1 = app.movements.first()
    val mov2 = app.movements.last()
    val res =
      getIntegrationData(APPEARANCE_MOVEMENTS_URL, app.id).successResponse<IntegrationResponses<IntegrationMovement>>()
    with(res.data.first()) {
      data verifyAgainst mov1
      assertThat(previousUrl).isEqualTo(BASE_TEST_URL + (APPEARANCE_URL from app.id))
      assertThat(nextUrl).isNull()
    }
    with(res.data.last()) {
      data verifyAgainst mov2
      assertThat(previousUrl).isEqualTo(BASE_TEST_URL + (APPEARANCE_URL from app.id))
      assertThat(nextUrl).isNull()
    }
    assertThat(res.previousUrl).isEqualTo(BASE_TEST_URL + (APPEARANCE_URL from app.id))
  }

  @Test
  fun `can retrieve a scheduled movement`() {
    val app = givenCourtAppearance(
      courtAppearance(
        movements = listOf(
          movement(CourtAppearanceMovement.Direction.OUT),
        ),
      ),
    )
    val mov = app.movements.single()
    val res = getIntegrationData(MOVEMENT_URL, mov.id).successResponse<IntegrationResponse<IntegrationMovement>>()
    res.data verifyAgainst mov
    assertThat(res.previousUrl).isEqualTo(BASE_TEST_URL + (APPEARANCE_URL from app.id))
    assertThat(res.nextUrl).isNull()
  }

  @Test
  fun `can retrieve an unscheduled movement`() {
    val mov = givenUnscheduledMovement(unscheduledMovement())
    val res = getIntegrationData(MOVEMENT_URL, mov.id).successResponse<IntegrationResponse<IntegrationMovement>>()
    res.data verifyAgainst mov
    assertThat(res.previousUrl).isNull()
    assertThat(res.nextUrl).isNull()
  }

  private fun getIntegrationData(
    url: String,
    id: UUID = newUuid(),
    role: String? = listOf(Roles.SCHEDULER_RO, Roles.SCHEDULER_RW).random(),
  ) = webTestClient
    .get()
    .uri(url, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  private infix fun IntegrationAppearance.verifyAgainst(appearance: CourtAppearance) {
    assertThat(id).isEqualTo(appearance.id)
    assertThat(personIdentifier).isEqualTo(appearance.person.identifier)
    assertThat(prisonCode).isEqualTo(appearance.prisonCode)
    assertThat(courtCode).isEqualTo(appearance.courtCode)
    assertThat(reason.code to reason.description).isEqualTo(appearance.reason.code to appearance.reason.description)
    assertThat(start.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(appearance.start.truncatedTo(ChronoUnit.SECONDS))
    assertThat(end?.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(appearance.end?.truncatedTo(ChronoUnit.SECONDS))
    assertThat(comments).isEqualTo(appearance.comments)
  }

  private infix fun IntegrationMovement.verifyAgainst(movement: CourtAppearanceMovement) {
    assertThat(id).isEqualTo(movement.id)
    assertThat(personIdentifier).isEqualTo(movement.person.identifier)
    assertThat(prisonCode).isEqualTo(movement.prisonCode)
    assertThat(courtCode).isEqualTo(movement.courtCode)
    assertThat(reason.code to reason.description).isEqualTo(movement.reason.code to movement.reason.description)
    assertThat(occurredAt.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(movement.occurredAt.truncatedTo(ChronoUnit.SECONDS))
    assertThat(comments).isEqualTo(movement.comments)
  }

  companion object {
    private const val BASE_TEST_URL = "http://local-api/"
    private const val APPEARANCE_URL = "integrations/court-appearances/{id}"
    private const val APPEARANCE_MOVEMENTS_URL = "integrations/court-appearances/{id}/movements"
    private const val MOVEMENT_URL = "integrations/court-appearance-movements/{id}"

    @JvmStatic
    fun allUrls() = listOf(APPEARANCE_URL, APPEARANCE_MOVEMENTS_URL, MOVEMENT_URL)

    private infix fun String.from(id: UUID): String = replace("{id}", id.toString())
  }
}
