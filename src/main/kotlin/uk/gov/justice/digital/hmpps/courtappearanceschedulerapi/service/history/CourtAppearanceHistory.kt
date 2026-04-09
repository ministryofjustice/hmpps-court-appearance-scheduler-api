package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.history

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AuditedAction

@Service
class CourtAppearanceHistory(
  entityManager: EntityManager,
  managerUsers: ManageUsersClient,
) : HistoryService<CourtAppearance>(entityManager, managerUsers) {
  override val entityClass = CourtAppearance::class.java
  override fun CourtAppearance.changesFrom(previous: CourtAppearance): List<AuditedAction.Change> = CourtAppearance.changeableProperties().mapNotNull {
    val change = it.invoke(this).asChangeValue()
    val previous = it.invoke(previous).asChangeValue()
    if (change != previous) {
      AuditedAction.Change(it.name, previous, change)
    } else {
      null
    }
  }
}
