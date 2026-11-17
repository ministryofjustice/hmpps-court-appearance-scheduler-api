package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.history

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AuditedAction

@Service
class CourtAppearanceHistory(
  transactionTemplate: TransactionTemplate,
  entityManager: EntityManager,
  managerUsers: ManageUsersClient,
) : HistoryService<CourtAppearance>(transactionTemplate, entityManager, managerUsers) {
  override val entityClass = CourtAppearance::class.java
  override fun Any.loadRelatedEntities() {
    if (this is CourtAppearance) {
      // call these properties for lazy loading
      this.courtCode.code
      this.reason.description
    }
  }
  override fun CourtAppearance.changesFrom(previous: CourtAppearance): List<AuditedAction.Change> = CourtAppearance.changeableProperties().mapNotNull {
    val change = it(this).asChangeValue()
    val previous = it(previous).asChangeValue()
    if (change != previous) {
      AuditedAction.Change(it.name, previous, change)
    } else {
      null
    }
  }
}
