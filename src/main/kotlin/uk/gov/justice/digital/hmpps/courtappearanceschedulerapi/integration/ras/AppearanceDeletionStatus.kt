package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras

enum class AppearanceDeletionStatus {
  SUPPORTED,
  NOT_SUPPORTED,
}

data class AppearanceDeletionStatusResponse(val status: AppearanceDeletionStatus)
