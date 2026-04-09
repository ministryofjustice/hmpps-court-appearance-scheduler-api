package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReason
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getStatusByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

typealias PersonProvider = (String, String) -> PersonSummary
typealias StatusProvider = (CourtAppearanceStatus.Code) -> CourtAppearanceStatus
typealias ReasonProvider = (String) -> CourtAppearanceReason
typealias CourtAppearanceProvider = (PersonProvider, ReasonProvider, StatusProvider) -> CourtAppearance

interface CourtAppearanceOperations {
  fun givenCourtAppearance(caProvider: CourtAppearanceProvider): CourtAppearance
  fun findCourtAppearance(id: UUID): CourtAppearance?

  companion object {
    fun courtAppearance(
      personIdentifier: String = personIdentifier(),
      prisonCode: String = prisonCode(),
      courtCode: String = courtCode(),
      reasonCode: String = "CRT",
      start: LocalDateTime = LocalDateTime.of(LocalDate.now().plusDays(7), LocalTime.of(10, 0, 0)),
      end: LocalDateTime? = LocalDateTime.of(LocalDate.now().plusDays(7), LocalTime.of(17, 0, 0)),
      comments: String? = word(25),
      legacyId: Long? = null,
      movements: List<CourtAppearanceMovement> = listOf(),
    ): CourtAppearanceProvider = { person, reason, status ->
      CourtAppearance(
        person(personIdentifier, prisonCode),
        prisonCode,
        courtCode,
        reason(reasonCode),
        start,
        end,
        comments,
        legacyId,
      )
        .apply { movements.forEach { addMovement(it) } }
        .calculateStatus(status)
    }
  }
}

class CourtAppearanceOperationsImpl(
  private val transactionTemplate: TransactionTemplate,
  private val reasonRepository: CourtAppearanceReasonRepository,
  private val statusRepository: CourtAppearanceStatusRepository,
  private val appearanceRepository: CourtAppearanceRepository,
  private val psOperations: PersonSummaryOperations,
) : CourtAppearanceOperations {
  override fun givenCourtAppearance(caProvider: CourtAppearanceProvider): CourtAppearance = transactionTemplate.execute {
    appearanceRepository.save(
      caProvider(
        { personIdentifier, prisonCode ->
          psOperations.findPersonSummary(personIdentifier)
            ?: psOperations.givenPersonSummary(personSummary(personIdentifier, prisonCode = prisonCode))
        },
        reasonRepository::getReasonByCode,
        statusRepository::getStatusByCode,
      ),
    )
  }

  override fun findCourtAppearance(id: UUID): CourtAppearance? = appearanceRepository.findByIdOrNull(id)
}
