package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.ServiceConfig
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.set
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getStatusByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearancePushSingle
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.InternalEventEmitter
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.RemandAndSentencingClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ExternalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.CancelAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ChangeAppearanceComments
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.CourtAppearanceAction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.CourtAppearanceActions
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RecategoriseAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RelocateAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RescheduleAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.history.CourtAppearanceHistory
import java.util.UUID

@Service
class CourtAppearanceModifications(
  private val transactionTemplate: TransactionTemplate,
  private val iee: InternalEventEmitter,
  private val serviceConfig: ServiceConfig,
  private val reasonRepository: CourtAppearanceReasonRepository,
  private val statusRepository: CourtAppearanceStatusRepository,
  private val appearanceRepository: CourtAppearanceRepository,
  private val appearanceHistory: CourtAppearanceHistory,
  private val rasClient: RemandAndSentencingClient,
) {
  fun apply(id: UUID, request: CourtAppearanceActions): AuditHistory {
    SchedulerContext.get().copy(reason = request.reason).set()
    val (readVersion, writeVersion, externalReference) = transactionTemplate.execute {
      val appearance = appearanceRepository.getAppearance(id)
      val readVersion = appearance.version
      request.actions.forEach { appearance.applyAction(it) }
      appearanceRepository.flush()
      UpdateAppearanceDetails(readVersion!!, appearance.version!!, appearance.externalReference)
    }
    externalReference?.takeIf { serviceConfig.ras.sendUpdates }
      ?.also { iee.publishInternalEvent(CourtAppearancePushSingle(it)) }
    return AuditHistory(listOfNotNull(appearanceHistory.currentAction(id, readVersion, writeVersion)))
  }

  private fun CourtAppearance.applyAction(action: CourtAppearanceAction) {
    when (action) {
      is ChangeAppearanceComments -> applyComments(action)
      is RecategoriseAppearance -> recategorise(action, reasonRepository::getReasonByCode)
      is RelocateAppearance -> relocate(action)
      is RescheduleAppearance -> reschedule(action).calculateStatus(statusRepository::getStatusByCode)
      is CancelAppearance -> handleCancel()

      else -> throw IllegalArgumentException("${action::class.simpleName} not supported")
    }
  }

  private fun CourtAppearance.canBeDeleted(): Boolean {
    val rasId = externalReference?.takeIf { it.service == ExternalReferenceService.REMAND_AND_SENTENCING }?.uuid
    return status.code == CourtAppearanceStatus.Code.SCHEDULED &&
      when (rasId) {
        null -> true
        else -> rasClient.deleteAppearance(rasId)
      }
  }

  private fun CourtAppearance.handleCancel() {
    if (canBeDeleted()) {
      appearanceRepository.delete(this)
    } else {
      throw ConflictException("Appearance cannot be deleted.")
    }
  }
}

private data class UpdateAppearanceDetails(
  val readVersion: Int,
  val writeVersion: Int,
  val externalReference: ExternalReference?,
)
