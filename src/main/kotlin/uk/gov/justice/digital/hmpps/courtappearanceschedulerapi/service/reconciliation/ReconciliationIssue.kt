package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.reconciliation

import java.util.UUID

sealed interface ReconciliationIssue {
  val personIdentifier: String
  val name: String
  fun telemetryProperties(): Map<String, String> = mapOf("personIdentifier" to personIdentifier)
}

data class OverallCountMismatch(
  override val personIdentifier: String,
  val casCount: Int,
  val rasCount: Int,
  val casMissing: Set<UUID>,
  val rasMissing: Set<UUID>,
) : ReconciliationIssue {
  override val name: String = "Overall Count Mismatch"
  override fun telemetryProperties(): Map<String, String> = super.telemetryProperties() + mapOf(
    "casCount" to casCount.toString(),
    "rasCount" to rasCount.toString(),
    "casMissing" to casMissing.joinToString(", "),
    "rasMissing" to rasMissing.joinToString(", "),
  )
}

data class PropertyMismatch(
  override val personIdentifier: String,
  val casId: UUID,
  val rasId: UUID,
  val propertyName: String,
) : ReconciliationIssue {
  override val name: String = "Property Mismatch"
  override fun telemetryProperties(): Map<String, String> = super.telemetryProperties() + mapOf(
    "propertyName" to propertyName,
    "casId" to casId.toString(),
    "rasId" to rasId.toString(),
  )
}

data class PersonIdentifierMismatch(
  override val personIdentifier: String,
  val otherIdentifiers: Set<String>,
) : ReconciliationIssue {
  override val name: String = "Person Identifier Mismatch"
  override fun telemetryProperties(): Map<String, String> = super.telemetryProperties() + mapOf("otherIdentifiers" to otherIdentifiers.joinToString(", "))
}
