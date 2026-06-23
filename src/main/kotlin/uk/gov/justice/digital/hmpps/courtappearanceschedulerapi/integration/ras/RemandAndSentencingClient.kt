package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
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

  fun canDeleteAppearance(uuid: UUID): Boolean = try {
    webClient.get()
      .uri("/court-appearance-schedule/{uuid}/delete-status", uuid)
      .retrieve()
      .bodyToMono<AppearanceDeletionStatusResponse>()
      .retryOnTransientException()
      .block()!!.status == AppearanceDeletionStatus.SUPPORTED
  } catch (_: Exception) {
    false
  }

  fun deleteAppearance(uuid: UUID): Boolean = webClient.delete().uri("/court-appearance/{uuid}", uuid)
    .exchangeToMono {
      when {
        it.statusCode().is2xxSuccessful || it.statusCode() == HttpStatus.NOT_FOUND -> Mono.just(true)
        else -> Mono.just(false)
      }
    }
    .retryOnTransientException()
    .block()!!
}
