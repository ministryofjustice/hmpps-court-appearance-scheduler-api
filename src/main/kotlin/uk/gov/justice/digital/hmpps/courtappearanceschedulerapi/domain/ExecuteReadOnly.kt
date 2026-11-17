package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate

fun <T> TransactionTemplate.executeReadOnly(action: (TransactionStatus) -> T): T? {
  val readOnlyTemplate = TransactionTemplate(transactionManager!!).apply {
    isReadOnly = true
    propagationBehavior = this@executeReadOnly.propagationBehavior
    isolationLevel = this@executeReadOnly.isolationLevel
    timeout = this@executeReadOnly.timeout
  }
  return readOnlyTemplate.execute(action)
}
