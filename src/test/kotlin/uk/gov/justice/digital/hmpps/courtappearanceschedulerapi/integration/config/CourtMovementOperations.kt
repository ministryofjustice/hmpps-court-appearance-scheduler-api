package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement.Direction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovementRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ReasonProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

typealias MovementProvider = (PersonProvider, ReasonProvider) -> CourtAppearanceMovement

interface CourtMovementOperations {
  fun givenUnscheduledMovement(unscheduled: MovementProvider): CourtAppearanceMovement
  fun findCourtMovement(id: UUID): CourtAppearanceMovement?

  companion object {
    fun unscheduledMovement(
      personIdentifier: String = personIdentifier(),
      prisonCode: String = prisonCode(),
      courtCode: String = courtCode(),
      direction: Direction = Direction.OUT,
      reasonCode: String = "CRT",
      occurredAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
      comments: String? = word(10) + " " + word(10),
      legacyId: String? = null,
      id: UUID? = newUuid(),
    ): MovementProvider = { person, reason ->
      CourtAppearanceMovement(
        null,
        person(personIdentifier, prisonCode),
        prisonCode,
        courtCode,
        reason(reasonCode),
        direction,
        occurredAt,
        comments,
        legacyId,
      )
    }
  }
}

class CourtMovementOperationsImpl(
  private val transactionTemplate: TransactionTemplate,
  private val reasonRepository: CourtAppearanceReasonRepository,
  private val movementRepository: CourtAppearanceMovementRepository,
  private val psOperations: PersonSummaryOperations,
) : CourtMovementOperations {
  override fun givenUnscheduledMovement(unscheduled: MovementProvider): CourtAppearanceMovement = transactionTemplate.execute {
    movementRepository.save(
      unscheduled(
        { personIdentifier, prisonCode ->
          psOperations.findPersonSummary(personIdentifier)
            ?: psOperations.givenPersonSummary(personSummary(personIdentifier, prisonCode = prisonCode))
        },
        reasonRepository::getReasonByCode,
      ),
    )
  }

  override fun findCourtMovement(id: UUID): CourtAppearanceMovement? = movementRepository.findByIdOrNull(id)
}
