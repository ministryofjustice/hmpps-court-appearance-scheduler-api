package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model

import com.fasterxml.jackson.annotation.JsonAlias

data class Court(@JsonAlias("courtId") val code: String, @JsonAlias("courtName") val name: String) {
  companion object {
    fun default(code: String): Court = Court(code = code, name = code)
  }
}
