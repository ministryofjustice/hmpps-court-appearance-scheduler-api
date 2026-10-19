package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtCodeRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReasonProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.StatusProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceMovement.Direction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.getStatusByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.externalreference.ExternalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.CourtRegisterMockServer.Companion.court
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Court
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

typealias PersonProvider = (String) -> PersonSummary
typealias CourtSortProvider = (String, String) -> CourtCode
typealias CourtAppearanceProvider = (PersonProvider, ReasonProvider, StatusProvider, CourtSortProvider) -> CourtAppearance

interface CourtAppearanceOperations {
  fun givenCourtAppearance(caProvider: CourtAppearanceProvider): CourtAppearance
  fun findCourtAppearance(id: UUID): CourtAppearance?
  fun findByExternalReference(externalReference: ExternalReference): CourtAppearance?
  fun givenCourtSort(code: String, name: String): CourtCode

  companion object {
    fun courtAppearance(
      personIdentifier: String = personIdentifier(),
      court: Court = court(),
      reasonCode: String = "CRT",
      start: LocalDateTime = LocalDate.now().plusDays(7).atTime(6, 0),
      end: LocalDateTime = maxOf(start.toLocalDate().atTime(17, 0), start),
      comments: String? = word(25),
      externalReference: ExternalReference? = null,
      legacyId: Long? = null,
      movements: List<(CourtAppearance) -> CourtAppearanceMovement> = listOf(),
      unschedule: Boolean = false,
    ): CourtAppearanceProvider = { person, reason, status, cs ->
      val p = person(personIdentifier)
      CourtAppearance(
        p,
        cs(court.code, court.name),
        reason(reasonCode),
        start.truncatedTo(ChronoUnit.SECONDS),
        end.truncatedTo(ChronoUnit.SECONDS),
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
    CourtAppearanceMovement(
      ca,
      ca.person,
      ca.person.prisonCode!!,
      ca.courtCode.code,
      ca.reason,
      direction,
      occurredAt,
      comments,
      legacyId,
    )
  }
}

class CourtAppearanceOperationsImpl(
  private val transactionTemplate: TransactionTemplate,
  private val reasonRepository: CourtAppearanceReasonRepository,
  private val statusRepository: CourtAppearanceStatusRepository,
  private val appearanceRepository: CourtAppearanceRepository,
  private val courtCodeRepository: CourtCodeRepository,
  private val psOperations: PersonSummaryOperations,
) : CourtAppearanceOperations {
  override fun givenCourtAppearance(caProvider: CourtAppearanceProvider): CourtAppearance = transactionTemplate.execute {
    val ca = caProvider(
      { personIdentifier ->
        psOperations.findPersonSummary(personIdentifier) ?: psOperations.givenPersonSummary(
          personSummary(
            personIdentifier,
          ),
        )
      },
      reasonRepository::getReasonByCode,
      statusRepository::getStatusByCode,
      { c, n -> givenCourtSort(c, n) },
    )
    appearanceRepository.save(ca)
  }

  override fun findCourtAppearance(id: UUID): CourtAppearance? = appearanceRepository.findByIdOrNull(id)

  override fun findByExternalReference(externalReference: ExternalReference): CourtAppearance? = appearanceRepository.findByExternalReference(externalReference)

  override fun givenCourtSort(code: String, name: String): CourtCode = courtCodeRepository.save(CourtCode(code, name))
}
