package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceCancelled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.PersonReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.RasAppearanceDeleted
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.RasAppearanceInformation
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.externalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.ras.CourtAppearanceDeletedHandler
import java.util.UUID

class RasCourtAppearanceDeletedIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired private val cadHandler: CourtAppearanceDeletedHandler,
) : IntegrationTest(),
  CourtAppearanceOperations by cao {

  @Test
  fun `no exceptions if records do not exist`() {
    val event = event()
    cadHandler.handle(event)
  }

  @Test
  fun `can delete a court appearance without movements`() {
    val toDelete = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    val event = event(toDelete.externalReference!!.uuid)
    cadHandler.handle(event)

    val deleted = findCourtAppearance(toDelete.id)
    assertThat(deleted).isNull()

    verifyAudit(
      toDelete,
      RevisionType.DEL,
      affectedEntities = setOf(HmppsDomainEvent::class.simpleName!!, CourtAppearance::class.simpleName!!),
    )

    verifyEventPublications(
      toDelete,
      setOf(
        CourtAppearanceCancelled(
          toDelete.person.identifier,
          toDelete.id,
          toDelete.externalReference,
        ).publication(toDelete.id),
      ),
    )
  }

  @Test
  fun `does not delete a court appearance with movements - ras reference removed`() {
    val noDelete = givenCourtAppearance(
      courtAppearance(
        externalReference = externalReference(),
        movements = listOf(movement(CourtAppearanceMovement.Direction.OUT)),
      ),
    )
    val event = event(noDelete.externalReference!!.uuid)
    cadHandler.handle(event)
    val updated = requireNotNull(findCourtAppearance(noDelete.id))
    assertThat(updated.externalReference).isNull()
  }
}

private fun event(
  appearanceId: UUID = UUID.randomUUID(),
  personIdentifier: String = personIdentifier(),
) = RasAppearanceDeleted(RasAppearanceInformation(appearanceId), PersonReference.withIdentifier(personIdentifier))
