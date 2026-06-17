package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import java.time.LocalDateTime
import java.util.UUID

data class Appearance(
  val id: UUID,
  val person: Person,
  val prison: Prison,
  val court: Court,
  val reason: AppearanceReason,
  val external: Boolean,
  val start: LocalDateTime,
  val end: LocalDateTime?,
  val comments: String?,
  val status: AppearanceStatus,
  val origin: AppearanceOrigin?,
)

data class AppearanceStatus(val code: CourtAppearanceStatus.Code, val description: String)
data class AppearanceOrigin(val source: OriginSource, val id: UUID) {
  data class OriginSource(val code: String, val name: String)

  companion object {
    private val KNOWN_SERVICES = setOf(
      OriginSource("remand-and-sentencing", "Remand and Sentencing"),
    )

    private fun findOrigin(code: String): OriginSource = checkNotNull(
      KNOWN_SERVICES.firstOrNull {
        code.equals(
          it.code,
          ignoreCase = true,
        )
      },
    ) { "Origin service not recognised" }

    operator fun invoke(urn: String): AppearanceOrigin {
      val urnParts = urn.split(":")
      val uuid = urnParts.last().toUuid()
      check(urnParts.size == 4 && uuid is UUID) { "URN does not match expected pattern: 'urn:service-name:entity-name:uuid'" }
      return AppearanceOrigin(findOrigin(urnParts[1]), uuid)
    }
  }
}

fun CourtAppearanceStatus.asStatus() = AppearanceStatus(code, description)
