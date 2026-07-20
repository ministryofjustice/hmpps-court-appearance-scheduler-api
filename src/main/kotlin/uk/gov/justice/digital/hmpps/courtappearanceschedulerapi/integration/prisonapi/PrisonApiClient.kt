package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.retryOnTransientException

@Component
class PrisonApiClient(
  @Qualifier("prisonApiWebClient") private val webClient: WebClient,
) {
  fun movementsFor(personIdentifier: String): List<PrisonerMovement> = webClient.get()
    .uri { builder ->
      builder.path(MOVEMENTS_URL)
      builder.queryParam("allBookings", true)
      builder.queryParam("movementTypes", PrisonerMovement.ADMISSION_TYPE, PrisonerMovement.RELEASE_TYPE)
      builder.build(personIdentifier)
    }.exchangeToMono { res ->
      when (res.statusCode()) {
        HttpStatus.NOT_FOUND -> Mono.just(emptyList())
        HttpStatus.OK -> res.bodyToMono<List<PrisonerMovement>>()
        else -> res.createError()
      }
    }
    .retryOnTransientException()
    .block()!!

  companion object {
    const val MOVEMENTS_URL = "/movements/offender/{personIdentifier}"
  }
}
