package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus.Code.UNSCHEDULED
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.RemandAndSentencingClient

@Service
class ReconcilePerson(
  private val prisonApi: PrisonApiClient,
  private val rasClient: RemandAndSentencingClient,
  private val caRepository: CourtAppearanceRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun reconcile(identifier: String) {
    val rasAppearances = rasClient.findScheduleAppearancesFor(identifier).courtAppearances
      .associateBy { it.id }
    val casAppearances = caRepository.findByPersonIdentifierAndExternalReferenceIsNotNull(identifier)
      .filter { it.externalReference!!.service == ExternalReferenceService.REMAND_AND_SENTENCING }
      .associateBy { it.externalReference!!.uuid }

    val issues = mutableListOf<ReconciliationIssue>()
    if (casAppearances.keys != rasAppearances.keys) {
      val casMissing = rasAppearances.keys - casAppearances.keys
      val rasMissing = casAppearances.keys - rasAppearances.keys
      issues += OverallCountMismatch(identifier, casAppearances.size, rasAppearances.size, casMissing, rasMissing)
    } else {
      val allKeys = rasAppearances.keys + casAppearances.keys
      issues += allKeys.flatMap { compare(rasAppearances[it], casAppearances[it]) }
    }

    issues.forEach {
      telemetryClient.trackEvent(it.name, it.telemetryProperties(), mapOf())
    }
  }

  private fun compare(ras: CourtAppearanceSchedule?, cas: CourtAppearance?): List<ReconciliationIssue> = if (ras == null || cas == null) {
    emptyList()
  } else {
    propertyReconcilers().mapNotNull {
      it.reconcile(ras, cas)?.let { propertyName ->
        PropertyMismatch(cas.person.identifier, cas.id, ras.id, propertyName)
      }
    }
  }

  private fun CourtAppearanceSchedule.prisonCodeAtStart(): String = prisonApi.mostRecentAdmissionBefore(personIdentifier, start)?.toAgency ?: "UKN"

  private fun propertyReconcilers(): List<PropertyReconciler> = listOf(
    PropertyReconciler("prisonCode", { it.prisonCodeAtStart() }, { it.prisonCode }),
    PropertyReconciler("courtCode", { it.courtCode }, { it.courtCode }),
    PropertyReconciler("start", { it.start }, { it.start }),
    PropertyReconciler("isDuplicate", { it.isDuplicate }, { it.status.code == UNSCHEDULED }),
    PropertyReconciler("reasonCode", { it.reason.code }, { it.reason.code }),
    PropertyReconciler("comments", { it.comments }, { it.comments }),
  )
}

class PropertyReconciler(
  val propertyName: String,
  val ras: (CourtAppearanceSchedule) -> Any?,
  val cas: (CourtAppearance) -> Any?,
) {
  fun reconcile(rasSchedule: CourtAppearanceSchedule, casSchedule: CourtAppearance): String? = if (ras(rasSchedule) == cas(casSchedule)) null else propertyName
}
