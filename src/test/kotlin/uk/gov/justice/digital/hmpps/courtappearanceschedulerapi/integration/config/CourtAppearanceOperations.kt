package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement.Direction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReasonProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.StatusProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getStatusByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ExternalReference
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

typealias PersonProvider = (String, String) -> PersonSummary
typealias CourtAppearanceProvider = (PersonProvider, ReasonProvider, StatusProvider) -> CourtAppearance

interface CourtAppearanceOperations {
  fun givenCourtAppearance(caProvider: CourtAppearanceProvider): CourtAppearance
  fun findCourtAppearance(id: UUID): CourtAppearance?
  fun findByExternalReference(externalReference: ExternalReference): CourtAppearance?

  companion object {
    fun courtAppearance(
      personIdentifier: String = personIdentifier(),
      prisonCode: String = prisonCode(),
      courtCode: String = courtCode(),
      reasonCode: String = "CRT",
      start: LocalDateTime = LocalDate.now().plusDays(7).atTime(6, 0),
      end: LocalDateTime? = maxOf(start.toLocalDate().atTime(17, 0), start),
      comments: String? = word(25),
      externalReference: ExternalReference? = null,
      legacyId: Long? = null,
      movements: List<(CourtAppearance) -> CourtAppearanceMovement> = listOf(),
      unschedule: Boolean = false,
    ): CourtAppearanceProvider = { person, reason, status ->
      CourtAppearance(
        person(personIdentifier, prisonCode),
        prisonCode,
        courtCode,
        reason(reasonCode),
        start.truncatedTo(ChronoUnit.SECONDS),
        end?.truncatedTo(ChronoUnit.SECONDS),
        comments,
        externalReference,
        legacyId,
      )
        .apply { movements.forEach { addMovement(it(this)) } }
        .calculateStatus(status, unscheduleOverride = unschedule)
    }
  }

  fun movement(
    direction: Direction,
    occurredAt: LocalDateTime = LocalDateTime.now(),
    comments: String? = word(25),
    legacyId: String? = null,
  ): (CourtAppearance) -> CourtAppearanceMovement = { ca ->
    CourtAppearanceMovement(ca, ca.person, ca.prisonCode, ca.courtCode, ca.reason, direction, occurredAt, comments, legacyId)
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

  override fun findByExternalReference(externalReference: ExternalReference): CourtAppearance? = appearanceRepository.findByExternalReference(externalReference)
}
