package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceEntity
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ExternalReference
import java.util.UUID
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicLong

object DataGenerator {
  private val id = AtomicLong(1)
  private val letters = ('A'..'Z')
  private val usedPrisonCodes = ConcurrentSkipListSet<String>()
  private val usedPersonIdentifiers = ConcurrentSkipListSet<String>()

  fun newId(): Long = id.getAndIncrement()
  fun personIdentifier(attempts: Int = 10): String {
    check(attempts > 0) { "Ran out of attempts to generate a unique person identifier" }
    val pi = "${letters.random()}${(1111..9999).random()}${letters.random()}${letters.random()}"
    return if (usedPersonIdentifiers.add(pi)) {
      pi
    } else {
      personIdentifier(attempts - 1)
    }
  }

  fun word(length: Int): String = (1..length).joinToString("") { if (it == 1) letters.random().uppercase() else letters.random().lowercase() }

  fun username(): String = (0..12).joinToString("") { letters.random().toString() }
  fun cellLocation(): String = "${letters.random()}-${(1..9).random()}-${(111..999).random()}"
  fun prisonCode(attempts: Int = 10): String {
    check(attempts > 0) { "Ran out of attempts to find a unique prison code" }
    val prisonCode = (1..3).map { letters.random() }.joinToString("")
    return if (usedPrisonCodes.add(prisonCode)) {
      prisonCode
    } else {
      prisonCode(attempts - 1)
    }
  }

  fun courtCode(): String = (1..6).map { letters.random() }.joinToString("")

  fun externalReference(
    service: ExternalReferenceService = ExternalReferenceService.REMAND_AND_SENTENCING,
    entity: ExternalReferenceEntity = ExternalReferenceEntity.COURT_APPEARANCE,
    uuid: UUID = newUuid(),
  ) = ExternalReference(service, entity, uuid)
}
