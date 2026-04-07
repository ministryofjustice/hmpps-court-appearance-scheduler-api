package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonregister

import com.fasterxml.jackson.annotation.JsonAlias
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.retryOnTransientException

@Service
class PrisonRegisterClient(
  @Qualifier("prisonRegisterApiWebClient") private val webClient: WebClient,
) {
  fun findPrisons(ids: Set<String>): List<Prison> = if (ids.isEmpty()) {
    emptyList()
  } else {
    webClient
      .post()
      .uri("/prisons/prisonsByIds")
      .bodyValue(PrisonsByIdsRequest(ids))
      .retrieve()
      .bodyToMono<List<Prison>>()
      .retryOnTransientException()
      .block()!!
  }

  fun findPrison(code: String): Prison? = findPrisons(setOf(code)).firstOrNull()
  fun getPrisonOrDefault(code: String): Prison = findPrison(code) ?: Prison.default(code)
}

data class PrisonsByIdsRequest(val prisonIds: Set<String>)

data class Prison(@JsonAlias("prisonId") val code: String, @JsonAlias("prisonName") val name: String) {
  companion object {
    fun default(code: String): Prison = Prison(code = code, name = code)
  }
}
