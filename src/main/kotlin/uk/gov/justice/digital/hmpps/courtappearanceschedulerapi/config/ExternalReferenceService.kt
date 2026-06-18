package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config

enum class ExternalReferenceService(val code: String, val description: String) {
  REMAND_AND_SENTENCING("remand-and-sentencing", "Remand and Sentencing"),
  ;

  companion object {
    @JvmStatic
    fun fromString(value: String): ExternalReferenceService = entries.firstOrNull { it.code.equals(value, ignoreCase = true) }
      ?: throw IllegalArgumentException("External reference service not recognised")
  }
}

enum class ExternalReferenceEntity(val code: String, val services: Set<ExternalReferenceService>) {
  COURT_APPEARANCE("court-appearance", setOf(ExternalReferenceService.REMAND_AND_SENTENCING)),
  ;

  fun forService(service: ExternalReferenceService) = apply {
    check(service in services) { "External reference service and entity combination not valid" }
  }

  companion object {
    @JvmStatic
    fun fromString(value: String): ExternalReferenceEntity = ExternalReferenceEntity.entries.firstOrNull { it.code.equals(value, ignoreCase = true) }
      ?: throw IllegalArgumentException("External reference entity not recognised")
  }
}
