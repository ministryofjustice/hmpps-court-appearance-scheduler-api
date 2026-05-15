package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.set
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReasonRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatusRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getReasonByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getStatusByCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.CancelAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.ChangeAppearanceComments
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.CourtAppearanceAction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RecategoriseAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RelocateAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RescheduleAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.history.CourtAppearanceHistory
import java.util.UUID

@Service
class CourtAppearanceModifications(
  private val transactionTemplate: TransactionTemplate,
  private val reasonRepository: CourtAppearanceReasonRepository,
  private val statusRepository: CourtAppearanceStatusRepository,
  private val appearanceRepository: CourtAppearanceRepository,
  private val appearanceHistory: CourtAppearanceHistory,
) {
  fun apply(id: UUID, action: CourtAppearanceAction): AuditHistory {
    SchedulerContext.get().copy(reason = action.reason).set()
    val (readVersion, writeVersion) = transactionTemplate.execute {
      val appearance = appearanceRepository.getAppearance(id)
      val readVersion = appearance.version
      when (action) {
        is ChangeAppearanceComments -> appearance.applyComments(action)
        is RecategoriseAppearance -> appearance.recategorise(action, reasonRepository::getReasonByCode)
        is RelocateAppearance -> appearance.relocate(action)
        is RescheduleAppearance -> appearance.reschedule(action).calculateStatus(statusRepository::getStatusByCode)
        is CancelAppearance -> if (appearance.status.code == CourtAppearanceStatus.Code.SCHEDULED) {
          appearanceRepository.delete(appearance)
        } else {
          throw ConflictException("Cannot delete an appearance that is not scheduled.")
        }
        else -> throw IllegalArgumentException("${action::class.simpleName} not supported")
      }
      appearanceRepository.flush()
      readVersion!! to appearance.version!!
    }
    return AuditHistory(listOfNotNull(appearanceHistory.currentAction(id, readVersion, writeVersion)))
  }
}
