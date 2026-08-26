package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.events

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
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
}

private fun event(
  personIdentifier: String = personIdentifier(),
  movementType: String = "ADM",
  directionCode: String = "IN",
  prisonCode: String = prisonCode(),
  occurredAt: LocalDateTime = LocalDateTime.now(),
) = ExternalMovementRecordedEvent(personIdentifier, movementType, directionCode, prisonCode, occurredAt)
