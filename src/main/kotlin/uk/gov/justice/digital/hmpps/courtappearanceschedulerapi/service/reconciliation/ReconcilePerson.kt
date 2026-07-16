package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceEntity.COURT_APPEARANCE
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService.REMAND_AND_SENTENCING
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.locationAt
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.RemandAndSentencingClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ExternalReference
import java.time.LocalDateTime
import java.util.UUID

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
      .filter { it.externalReference!!.service == REMAND_AND_SENTENCING }
      .associateBy { it.externalReference!!.uuid }

    val issues = mutableListOf<ReconciliationIssue>()
    val (rasFound, casFound) = if (casAppearances.keys != rasAppearances.keys) {
      val casMissingIds = rasAppearances.keys - casAppearances.keys
      val rasMissingIds = casAppearances.keys - rasAppearances.keys
      val casFound = if (casMissingIds.isEmpty()) {
        emptyList()
      } else {
        caRepository.findByExternalReferenceIn(casMissingIds.map(::rasReference).toSet())
      }
      val rasFound = rasClient.findCourtAppearanceSchedules(rasMissingIds).courtAppearances
      val casFoundIds = casFound.map { it.externalReference!!.uuid }.toSet()
      val rasFoundIds = rasFound.map { it.id }.toSet()
      issues += if (casFoundIds.containsAll(casMissingIds) && rasFoundIds.containsAll(rasMissingIds)) {
        PersonIdentifierMismatch(
          identifier,
          (casFound.map { it.person.identifier } + rasFound.map { it.personIdentifier }).toSet(),
        )
      } else {
        val casNotFound = casMissingIds - casFoundIds
        val rasNotFound = rasMissingIds - rasFoundIds
        OverallCountMismatch(
          identifier,
          casNotFound.size,
          rasNotFound.size,
          casNotFound,
          rasNotFound,
        )
      }
      rasFound.associateBy { it.id } to casFound.associateBy { it.externalReference!!.uuid }
    } else {
      emptyMap<UUID, CourtAppearanceSchedule>() to emptyMap()
    }

    val allRas = rasAppearances + rasFound
    val allCas = casAppearances + casFound
    val allKeys = rasAppearances.keys + casAppearances.keys
    if (allKeys.isNotEmpty()) {
      val movements = prisonApi.movementsFor(identifier)
      issues += allKeys.flatMap { compare(allRas[it], allCas[it], movements::locationAt) }
    }

    issues.forEach {
      telemetryClient.trackEvent(it.name, it.telemetryProperties(), mapOf())
    }
  }

  private fun rasReference(uuid: UUID): ExternalReference = ExternalReference(REMAND_AND_SENTENCING, COURT_APPEARANCE, uuid)

  private fun compare(
    ras: CourtAppearanceSchedule?,
    cas: CourtAppearance?,
    locationAt: (LocalDateTime) -> String,
  ): List<ReconciliationIssue> = if (ras == null || cas == null) {
    emptyList()
  } else {
    propertyReconcilers { start -> locationAt(start) }
      .mapNotNull {
        it.reconcile(ras, cas)?.let { propertyName ->
          PropertyMismatch(cas.person.identifier, cas.id, ras.id, propertyName)
        }
      }
  }

  private fun propertyReconcilers(prisonCodeAt: (LocalDateTime) -> String): List<PropertyReconciler> = listOf(
    DuplicateReconciler(),
    EqualityReconciler("prisonCode", { prisonCodeAt(it.start) }, { it.prisonCode }),
    EqualityReconciler("courtCode", { it.courtCode }, { it.courtCode }),
    EqualityReconciler("start", { it.start }, { it.start }),
    EqualityReconciler("reasonCode", { it.reason.code }, { it.reason.code }),
    EqualityReconciler("comments", { it.comments }, { it.comments }),
  )
}
