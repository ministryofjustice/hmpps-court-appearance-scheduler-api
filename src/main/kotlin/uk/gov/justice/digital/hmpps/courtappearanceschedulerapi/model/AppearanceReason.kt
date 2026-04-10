package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceReason

data class AppearanceReason(val code: String, val description: String, val external: Boolean)

data class CourtAppearanceReasons(val items: List<AppearanceReason>)

fun CourtAppearanceReason.asReason() = AppearanceReason(code, description, external)
