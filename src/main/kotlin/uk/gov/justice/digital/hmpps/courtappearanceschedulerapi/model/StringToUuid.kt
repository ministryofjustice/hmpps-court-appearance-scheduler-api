package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model

import java.util.UUID

fun String.toUuid(): UUID? = try {
  UUID.fromString(this)
} catch (_: IllegalArgumentException) {
  null
}
