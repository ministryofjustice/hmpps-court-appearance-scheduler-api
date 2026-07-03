package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config

import org.assertj.core.api.Assertions.assertThat
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.cellLocation
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonersearch.Prisoner

interface PersonSummaryOperations {
  fun givenPersonSummary(personSummary: PersonSummary): PersonSummary
  fun findPersonSummary(personIdentifier: String): PersonSummary?
  fun findAllPeople(): List<PersonSummary>

  companion object {
    fun personSummary(
      personIdentifier: String = personIdentifier(),
      firstName: String = word(8),
      lastName: String = word(8),
      prisonCode: String? = prisonCode(),
      cellLocation: String? = cellLocation(),
    ): PersonSummary = PersonSummary(firstName, lastName, prisonCode, cellLocation, personIdentifier)

    fun PersonSummary.verifyAgainst(prisoner: Prisoner) {
      assertThat(identifier).isEqualTo(prisoner.prisonerNumber)
      assertThat(firstName).isEqualTo(prisoner.firstName)
      assertThat(lastName).isEqualTo(prisoner.lastName)
      assertThat(prisonCode).isEqualTo(prisoner.prisonId)
      assertThat(cellLocation).isEqualTo(prisoner.cellLocation)
    }
  }
}

class PersonSummaryOperationsImpl(
  private val personSummaryRepository: PersonSummaryRepository,
) : PersonSummaryOperations {
  override fun givenPersonSummary(personSummary: PersonSummary): PersonSummary = personSummaryRepository.save(personSummary)

  override fun findPersonSummary(personIdentifier: String): PersonSummary? = personSummaryRepository.findByIdOrNull(personIdentifier)

  override fun findAllPeople(): List<PersonSummary> = personSummaryRepository.findAll()
}
