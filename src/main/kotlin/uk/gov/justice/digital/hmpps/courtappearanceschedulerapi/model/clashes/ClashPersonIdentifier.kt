package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes

data class ClashPersonIdentifier(val type: Type, val value: String) {
  enum class Type {
    PRISON_NUMBER,
  }
}
