package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.PrisonerMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.PrisonerMovement.Companion.DEFAULT_LOCATION
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.locationAt
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonApiMockServer.Companion.prisonerMovement
import java.time.LocalDateTime

class PrisonerMovementLocationAtTest {
  @ParameterizedTest
  @MethodSource("movementHistory")
  fun `correct movement identified for date time`(
    movements: List<PrisonerMovement>,
    dateTime: LocalDateTime,
    expectedLocation: String,
  ) {
    assertThat(movements.locationAt(dateTime)).isEqualTo(expectedLocation)
  }

  companion object {
    private val movements: List<PrisonerMovement> = listOf(
      prisonerMovement(
        movementType = PrisonerMovement.ADMISSION_TYPE,
        dateTime = LocalDateTime.of(2024, 3, 2, 10, 45),
        toAgency = "AAA",
      ),
      prisonerMovement(
        movementType = PrisonerMovement.RELEASE_TYPE,
        dateTime = LocalDateTime.of(2024, 9, 1, 15, 20),
        toAgency = "OUT",
      ),
      prisonerMovement(
        movementType = PrisonerMovement.ADMISSION_TYPE,
        dateTime = LocalDateTime.of(2025, 6, 4, 8, 0),
        toAgency = "BBB",
      ),
      prisonerMovement(
        movementType = "CRT",
        dateTime = LocalDateTime.of(2025, 7, 3, 14, 30),
        toAgency = "BBB",
      ),
      prisonerMovement(
        movementType = "TRN",
        dateTime = LocalDateTime.of(2025, 8, 1, 10, 0),
        toAgency = "CCC",
      ),
      prisonerMovement(
        movementType = PrisonerMovement.RELEASE_TYPE,
        dateTime = LocalDateTime.of(2025, 12, 13, 14, 15),
        toAgency = "OUT",
      ),
      prisonerMovement(
        movementType = PrisonerMovement.ADMISSION_TYPE,
        dateTime = LocalDateTime.now(),
        toAgency = "DDD",
      ),
    )

    @JvmStatic
    fun movementHistory() = listOf(
      Arguments.of(movements, LocalDateTime.now(), "DDD"),
      Arguments.of(movements, LocalDateTime.now().minusDays(1), DEFAULT_LOCATION),
      Arguments.of(movements, LocalDateTime.of(2024, 3, 1, 14, 0), DEFAULT_LOCATION),
      Arguments.of(movements, LocalDateTime.of(2024, 3, 2, 17, 0), "AAA"),
      Arguments.of(movements, LocalDateTime.of(2024, 9, 1, 12, 0), "AAA"),
      Arguments.of(movements, LocalDateTime.of(2025, 6, 3, 14, 0), DEFAULT_LOCATION),
      Arguments.of(movements, LocalDateTime.of(2025, 7, 2, 10, 30), "BBB"),
      Arguments.of(movements, LocalDateTime.of(2025, 10, 22, 10, 30), "BBB"),
    )
  }
}
