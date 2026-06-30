package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import io.sentry.Sentry
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtAppearanceReconcilePerson
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.InternalEventEmitter
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonersearch.PrisonerSearchClient

@Service
class ReconcilePrison(
  private val prisonerSearch: PrisonerSearchClient,
  private val iee: InternalEventEmitter,
) {
  fun reconcile(prisonCode: String) {
    try {
      iee.publishInternalEvents(prisonerSearch.findPrisonersFor(prisonCode).map { CourtAppearanceReconcilePerson(it.prisonerNumber) })
    } catch (e: Exception) {
      Sentry.captureException(e)
    } finally {
      SchedulerContext.clear()
    }
  }
}
