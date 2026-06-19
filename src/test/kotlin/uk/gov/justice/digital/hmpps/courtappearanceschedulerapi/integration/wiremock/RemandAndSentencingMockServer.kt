package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.http.HttpStatus
import tools.jackson.module.kotlin.jsonMapper
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.CourtAppearanceSchedulesRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.CourtAppearanceSchedulesResponse
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.WiremockConfig.mockServerConfig
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.CourtEvent
import java.time.LocalDateTime
import java.util.UUID

class RemandAndSentencingMockServer : WireMockServer(mockServerConfig(9007)) {
  fun givenCourtAppearanceSchedule(schedule: CourtAppearanceSchedule): CourtAppearanceSchedule = givenCourtAppearanceSchedules(listOf(schedule)).first()

  fun givenCourtAppearanceSchedules(schedules: List<CourtAppearanceSchedule>): List<CourtAppearanceSchedule> {
    val response = jsonMapper().writeValueAsString(CourtAppearanceSchedulesResponse(schedules))
    stubFor(
      post(urlPathEqualTo("/search/court-appearance-schedules"))
        .withRequestBody(
          equalToJson(
            jsonMapper().writeValueAsString(
              CourtAppearanceSchedulesRequest(schedules.map { it.id }.toSet()),
            ),
            true,
            true,
          ),
        )
        .withBearerToken()
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.OK.value())
            .withBody(response),
        ),
    )
    println(response)
    return schedules
  }

  companion object {
    fun caSchedule(
      id: UUID,
      personIdentifier: String,
      courtCode: String,
      reason: CourtAppearanceSchedule.ScheduleReason,
      start: LocalDateTime,
      isDuplicate: Boolean,
    ) = CourtAppearanceSchedule(id, personIdentifier, courtCode, reason, start, isDuplicate)
  }
}

fun CourtAppearance.schedule(isDuplicate: Boolean) = CourtAppearanceSchedule(
  externalReference?.uuid ?: newUuid(),
  person.identifier,
  courtCode,
  CourtAppearanceSchedule.ScheduleReason(reason.code),
  start,
  isDuplicate,
)

fun CourtEvent.schedule(personIdentifier: String) = externalReferenceUrn?.uuid?.let {
  CourtAppearanceSchedule(
    it,
    personIdentifier,
    scheduledCourtCode,
    CourtAppearanceSchedule.ScheduleReason(type),
    start,
    !currentTerm,
  )
}

class RemandAndSentencingExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val rasMockServer = RemandAndSentencingMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    rasMockServer.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    rasMockServer.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    rasMockServer.stop()
  }
}
