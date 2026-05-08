package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceMovementRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReason
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationReason
import java.util.UUID

@Service
class IntegrationRetriever(
  private val appearanceRepository: CourtAppearanceRepository,
  private val movementRepository: CourtAppearanceMovementRepository,
) {
  fun appearance(id: UUID): IntegrationAppearance = appearanceRepository.getAppearance(id).forIntegration()

  fun movementsForAppearance(id: UUID): List<IntegrationMovement> = movementRepository.findAllForAppearance(id).map { it.forIntegration() }

  fun movement(id: UUID): IntegrationMovement = movementRepository.getMovement(id).forIntegration()
}

private fun CourtAppearance.forIntegration() = IntegrationAppearance(
  id,
  person.identifier,
  prisonCode,
  courtCode,
  reason.forIntegration(),
  start,
  end,
  comments,
)

private fun CourtAppearanceMovement.forIntegration() = IntegrationMovement(
  id,
  courtAppearance?.id,
  person.identifier,
  prisonCode,
  courtCode,
  direction,
  reason.forIntegration(),
  occurredAt,
  comments,
)

private fun CourtAppearanceReason.forIntegration() = IntegrationReason(code, description)
