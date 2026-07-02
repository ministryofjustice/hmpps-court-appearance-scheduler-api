package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.retryOnTransientException
import java.time.LocalDateTime

@Component
class PrisonApiClient(
  @Qualifier("prisonApiWebClient") private val webClient: WebClient,
) {
  fun mostRecentAdmissionBefore(personIdentifier: String, before: LocalDateTime): PrisonerMovement? {
    val previousAdmission = movementsFor(personIdentifier)
      .filter { it.isAdmission() }
      .sortedByDescending { it.movementDateTime }
      .firstOrNull { before.isAfter(it.movementDateTime) }
    return previousAdmission
  }

  fun movementsFor(personIdentifier: String): List<PrisonerMovement> = webClient.get()
    .uri { builder ->
      builder.path(MOVEMENTS_URL)
      builder.queryParam("allBookings", true)
      builder.queryParam("movementTypes", PrisonerMovement.ADMISSION_TYPE)
      builder.build(personIdentifier)
    }.retrieve()
    .bodyToMono<List<PrisonerMovement>>()
    .retryOnTransientException()
    .block()!!

  companion object {
    const val MOVEMENTS_URL = "/movements/offender/{personIdentifier}"
  }
}
