package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceCloned
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceClonedInformation
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.PersonReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.externalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal.CourtAppearanceClonedHandler
import java.util.UUID
import java.util.UUID.randomUUID

class CourtAppearanceClonedIntTest(
  @Autowired pso: PersonSummaryOperations,
  @Autowired cao: CourtAppearanceOperations,
  @Autowired private val cacHandler: CourtAppearanceClonedHandler,
) : IntegrationTest(),
  PersonSummaryOperations by pso,
  CourtAppearanceOperations by cao {

  @Test
  fun `Illegal State Exception thrown if either appearance is missing`() {
    val ca = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    val e1 =
      assertThrows<IllegalStateException> { cacHandler.handle(event(ca.externalReference!!.uuid, randomUUID())) }

    assertThat(e1.message).isEqualTo("Both appearances must exist to process a clone event")

    val e2 =
      assertThrows<IllegalStateException> { cacHandler.handle(event(randomUUID(), ca.externalReference!!.uuid)) }

    assertThat(e2.message).isEqualTo("Both appearances must exist to process a clone event")
  }

  @Test
  fun `Can switch cloned and original external references`() {
    val orig = givenCourtAppearance(courtAppearance(externalReference = externalReference()))
    val clone = givenCourtAppearance(
      courtAppearance(
        personIdentifier = orig.person.identifier,
        externalReference = externalReference(),
      ),
    )

    cacHandler.handle(event(orig.externalReference!!.uuid, clone.externalReference!!.uuid, orig.person.identifier))

    val original = requireNotNull(findCourtAppearance(orig.id))
    assertThat(original.externalReference).isEqualTo(clone.externalReference)
    val cloned = requireNotNull(findCourtAppearance(clone.id))
    assertThat(cloned.externalReference).isEqualTo(orig.externalReference)
  }
}

private fun event(
  previousId: UUID = randomUUID(),
  clonedId: UUID = randomUUID(),
  personIdentifier: String = personIdentifier(),
) = CourtAppearanceCloned(
  CourtAppearanceClonedInformation(personIdentifier, previousId, clonedId),
  PersonReference.withIdentifier(personIdentifier),
)
