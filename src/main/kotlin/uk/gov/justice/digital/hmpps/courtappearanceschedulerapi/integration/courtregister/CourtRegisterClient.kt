package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.courtregister

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.retryOnTransientException
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Court

@Service
class CourtRegisterClient(
  @Qualifier("courtRegisterApiWebClient") private val webClient: WebClient,
) {
  fun findCourts(ids: Set<String>): Mono<List<Court>> = if (ids.isEmpty()) {
    Mono.just(emptyList())
  } else {
    webClient
      .get()
      .uri {
        it.path("/courts/id/multiple")
          .queryParam("courtIds", *ids.toTypedArray())
          .build()
      }
      .retrieve()
      .bodyToMono<List<Court>>()
      .retryOnTransientException()
  }

  fun findCourt(code: String): Mono<Court> = findCourts(setOf(code)).map { cts -> cts.firstOrNull { it.code == code } ?: Court.default(code) }
}

data class CourtsByIdsRequest(val courtIds: Set<String>)
