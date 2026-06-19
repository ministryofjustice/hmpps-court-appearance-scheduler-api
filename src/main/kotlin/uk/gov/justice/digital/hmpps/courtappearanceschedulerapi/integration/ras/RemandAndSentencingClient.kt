package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.ServiceConfig
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.retryOnTransientException
import java.util.UUID

@Service
class RemandAndSentencingClient(
  @Qualifier("remandAndSentencingWebClient") private val webClient: WebClient,
  private val serviceConfig: ServiceConfig,
) {
  fun findCourtAppearanceSchedules(ids: Set<UUID>): CourtAppearanceSchedulesResponse = if (ids.isEmpty() || !serviceConfig.enableRasClient) {
    CourtAppearanceSchedulesResponse(emptyList())
  } else {
    webClient
      .post()
      .uri("/search/court-appearance-schedules")
      .bodyValue(CourtAppearanceSchedulesRequest(ids))
      .retrieve()
      .bodyToMono<CourtAppearanceSchedulesResponse>()
      .retryOnTransientException()
      .block()!!
  }

  fun findCourtAppearanceSchedule(uuid: UUID): CourtAppearanceSchedule? = findCourtAppearanceSchedules(setOf(uuid)).courtAppearances.find { it.id == uuid }
}
