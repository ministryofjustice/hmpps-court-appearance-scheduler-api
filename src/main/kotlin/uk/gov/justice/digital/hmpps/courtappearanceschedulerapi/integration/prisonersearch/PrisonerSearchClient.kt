package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonersearch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.retryOnTransientException

@Component
class PrisonerSearchClient(
  @Qualifier("prisonerSearchWebClient") private val webClient: WebClient,
) {
  fun getPrisoners(prisonNumbers: Set<String>): List<Prisoner> = if (prisonNumbers.isEmpty()) {
    emptyList()
  } else {
    Flux
      .fromIterable(prisonNumbers)
      .buffer(PRISONER_SEARCH_LIMIT)
      .flatMap {
        webClient
          .post()
          .uri {
            it.path(GET_PRISONERS_BY_IDENTIFIER)
            it.queryParam("responseFields", *Prisoner.fields())
            it.build()
          }.bodyValue(PrisonerNumbers(prisonNumbers))
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
          .retrieve()
          .bodyToFlux<Prisoner>()
          .retryOnTransientException()
      }.collectList()
      .block()!!
  }

  fun getPrisoner(personIdentifier: String): Prisoner? = getPrisoners(setOf(personIdentifier)).firstOrNull {
    it.prisonerNumber == personIdentifier
  }

  fun findPrisonersFor(prisonCode: String): List<Prisoner> = webClient
    .get()
    .uri {
      it.path(GET_PRISONERS_FOR_PRISON)
      it.queryParam("size", PRISONER_SEARCH_LIMIT)
      it.queryParam("responseFields", *Prisoner.fields())
      it.build(prisonCode)
    }.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
    .retrieve()
    .bodyToMono<Prisoners>()
    .retryOnTransientException()
    .block()!!.content

  companion object {
    const val PRISONER_SEARCH_LIMIT = 10000
    const val GET_PRISONERS_BY_IDENTIFIER = "/prisoner-search/prisoner-numbers"
    const val GET_PRISONERS_FOR_PRISON = "/prisoner-search/prison/{prisonCode}"
  }
}

data class Prisoners(val content: List<Prisoner>)
