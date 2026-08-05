package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonregister

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.retryOnTransientException
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Prison

@Component
class PrisonRegisterClient(
  @Qualifier("prisonRegisterApiWebClient") private val webClient: WebClient,
) {
  fun findAllPrisons(active: Boolean? = true): List<Prison> = webClient.get().uri { ub ->
    ub.path("/prisons/names")
    active?.let { ub.queryParam("active", it) }
    ub.build()
  }.retrieve()
    .bodyToMono<List<Prison>>()
    .retryOnTransientException()
    .block()!!
}

data class PrisonsByIdsRequest(val prisonIds: Set<String>)
