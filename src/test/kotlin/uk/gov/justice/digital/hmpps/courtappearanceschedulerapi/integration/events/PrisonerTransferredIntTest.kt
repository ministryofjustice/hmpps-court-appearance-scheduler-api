package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.externalmovements.ExternalMovementHandler
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.externalmovements.ExternalMovementRecordedEvent
import java.time.LocalDateTime

class PrisonerTransferredIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired private val emHandler: ExternalMovementHandler,
) : IntegrationTest(),
  CourtAppearanceOperations by cao {

  @Test
  fun `no exceptions if records do not exist`() {
    val event = event()
    emHandler.handle(event)
  }

  @Test
  fun `future scheduled appearances switch to new prison`() {
    val scheduled = givenCourtAppearance(courtAppearance())
    val past = givenCourtAppearance(courtAppearance(start = LocalDateTime.now().minusDays(1)))
    val event = event(scheduled.person.identifier)
    emHandler.handle(event)

    val updated = requireNotNull(findCourtAppearance(scheduled.id))
    val notUpdated = requireNotNull(findCourtAppearance(past.id))

    assertThat(updated.prisonCode).isEqualTo(event.prisonCode)
    assertThat(notUpdated.prisonCode).isEqualTo(past.prisonCode)
  }
}

private fun event(
  personIdentifier: String = personIdentifier(),
  movementType: String = "ADM",
  directionCode: String = "IN",
  prisonCode: String = prisonCode(),
  occurredAt: LocalDateTime = LocalDateTime.now(),
) = ExternalMovementRecordedEvent(personIdentifier, movementType, directionCode, prisonCode, occurredAt)
