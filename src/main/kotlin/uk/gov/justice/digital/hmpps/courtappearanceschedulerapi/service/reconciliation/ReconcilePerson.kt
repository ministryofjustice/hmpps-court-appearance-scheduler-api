package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.RemandAndSentencingClient
import kotlin.reflect.KProperty

@Service
class ReconcilePerson(
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
}

private fun compare(ras: CourtAppearanceSchedule?, cas: CourtAppearance?): List<ReconciliationIssue> {
  val issues = mutableListOf<ReconciliationIssue>()

  if (ras != null && cas != null) {
    propertyMappings().map { e ->
      val rasProperty = e.key
      val casProperty = e.value
      if (rasProperty.getter.call(ras)?.equals(casProperty.getter.call(cas)) != true ||
        casProperty.getter.call(cas)?.equals(rasProperty.getter.call(ras)) != true
      ) {
        issues += PropertyMismatch(cas.person.identifier, cas.id, ras.id, casProperty.name, rasProperty.name)
      }
    }
  }

  return issues
}

fun propertyMappings(): Map<KProperty<*>, KProperty<*>> = mapOf(
  CourtAppearanceSchedule::courtCode to CourtAppearance::courtCode,
  CourtAppearanceSchedule::start to CourtAppearance::start,
  CourtAppearanceSchedule::comments to CourtAppearance::comments,
)
