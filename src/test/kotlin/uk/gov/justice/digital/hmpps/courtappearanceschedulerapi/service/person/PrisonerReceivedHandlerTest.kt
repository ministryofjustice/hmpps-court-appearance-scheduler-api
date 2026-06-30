package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.PersonReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.PrisonerReceived
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.PrisonerReceivedInformation
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.nomis.MigrationClient

@ExtendWith(MockitoExtension::class)
class PrisonerReceivedHandlerTest {
  @Mock
  lateinit var migrationClient: MigrationClient

  @InjectMocks
  lateinit var prisonerReceivedHandler: PrisonerReceivedHandler

  @Test
  fun `booking switch event calls migration repair (resync)`() {
    val event = prisonerReceivedEvent(PrisonerReceivedInformation.BOOKING_SWITCHED_REASON)
    prisonerReceivedHandler.handle(event)

    verify(migrationClient).requestRepair(event.additionalInformation.nomsNumber)
  }

  @Test
  fun `other prisoner received event is ignored`() {
    val event = prisonerReceivedEvent("ANY_OTHER_REASON")
    prisonerReceivedHandler.handle(event)

    verifyNoInteractions(migrationClient)
  }

  private fun prisonerReceivedEvent(
    reason: String,
    personIdentifier: String = personIdentifier(),
    prisonCode: String = prisonCode(),
  ) = PrisonerReceived(PrisonerReceivedInformation(personIdentifier, prisonCode, reason), PersonReference.withIdentifier(personIdentifier))
}
