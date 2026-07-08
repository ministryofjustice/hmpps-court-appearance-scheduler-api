package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.CourtAppearanceSchedule

interface PropertyReconciler {
  val propertyName: String
  fun reconcile(rasSchedule: CourtAppearanceSchedule, casSchedule: CourtAppearance): String?
}

class EqualityReconciler(
  override val propertyName: String,
  val ras: (CourtAppearanceSchedule) -> Any?,
  val cas: (CourtAppearance) -> Any?,
) : PropertyReconciler {
  override fun reconcile(rasSchedule: CourtAppearanceSchedule, casSchedule: CourtAppearance): String? = if (ras(rasSchedule) == cas(casSchedule)) null else propertyName
}

class DuplicateReconciler(
  override val propertyName: String = "isDuplicate",
) : PropertyReconciler {
  override fun reconcile(rasSchedule: CourtAppearanceSchedule, casSchedule: CourtAppearance): String? = if (rasSchedule.isDuplicate && casSchedule.status.code !in INACTIVE_STATES) {
    propertyName
  } else {
    null
  }

  companion object {
    private val INACTIVE_STATES = setOf(CourtAppearanceStatus.Code.UNSCHEDULED, CourtAppearanceStatus.Code.EXPIRED)
  }
}