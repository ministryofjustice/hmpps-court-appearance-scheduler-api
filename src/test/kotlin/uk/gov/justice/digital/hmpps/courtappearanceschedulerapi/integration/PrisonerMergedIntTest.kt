package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.PersonReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.PrisonerMerged
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.PrisonerMergedInformation
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtMovementOperations.Companion.unscheduledMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person.PrisonerMergedHandler

class PrisonerMergedIntTest(
  @Autowired cmo: CourtMovementOperations,
  @Autowired cao: CourtAppearanceOperations,
  @Autowired ps: PersonSummaryOperations,
  @Autowired private val mergedHandler: PrisonerMergedHandler,
) : IntegrationTest(),
  CourtMovementOperations by cmo,
  CourtAppearanceOperations by cao,
  PersonSummaryOperations by ps {

  @Test
  fun `prisoner merged event noop when no data`() {
    val fromPi = personIdentifier()
    val toPi = personIdentifier()

    mergedHandler.handle(prisonerMergedEvent(fromPi, toPi))
  }

  @Test
  fun `prisoner data merged on event for new identifier`() {
    val prisonCode = prisonCode()
    val fromPerson = givenPersonSummary(personSummary(prisonCode = prisonCode))
    val toPrisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode))
    val sch = givenCourtAppearance(
      courtAppearance(
        personIdentifier = fromPerson.identifier,
        prisonCode = prisonCode,
        movements = listOf(movement(CourtAppearanceMovement.Direction.OUT)),
      ),
    )
    val uns = givenUnscheduledMovement(
      unscheduledMovement(
        personIdentifier = fromPerson.identifier,
        prisonCode = prisonCode,
      ),
    )

    mergedHandler.handle(prisonerMergedEvent(fromPerson.identifier, toPrisoner.prisonerNumber))

    val scheduled = requireNotNull(findCourtAppearance(sch.id))
    assertThat(scheduled.person.identifier).isEqualTo(toPrisoner.prisonerNumber)

    val unscheduled = requireNotNull(findCourtMovement(uns.id))
    assertThat(unscheduled.person.identifier).isEqualTo(toPrisoner.prisonerNumber)

    assertThat(findPersonSummary(fromPerson.identifier)).isNull()
  }

  @Test
  fun `prisoner data merged on event existing identifier`() {
    val prisonCode = prisonCode()
    val fromPerson = givenPersonSummary(personSummary(prisonCode = prisonCode))
    val toPerson = givenPersonSummary(personSummary(prisonCode = prisonCode))
    val sch = givenCourtAppearance(
      courtAppearance(
        personIdentifier = fromPerson.identifier,
        prisonCode = prisonCode,
        movements = listOf(movement(CourtAppearanceMovement.Direction.OUT)),
      ),
    )
    val uns = givenUnscheduledMovement(
      unscheduledMovement(
        personIdentifier = fromPerson.identifier,
        prisonCode = prisonCode,
      ),
    )

    mergedHandler.handle(prisonerMergedEvent(fromPerson.identifier, toPerson.identifier))

    val scheduled = requireNotNull(findCourtAppearance(sch.id))
    assertThat(scheduled.person.identifier).isEqualTo(toPerson.identifier)

    val unscheduled = requireNotNull(findCourtMovement(uns.id))
    assertThat(unscheduled.person.identifier).isEqualTo(toPerson.identifier)

    assertThat(findPersonSummary(fromPerson.identifier)).isNull()
  }

  private fun prisonerMergedEvent(from: String, to: String): PrisonerMerged = PrisonerMerged(
    PrisonerMergedInformation(from, to),
    PersonReference.withIdentifier(to),
  )
}
