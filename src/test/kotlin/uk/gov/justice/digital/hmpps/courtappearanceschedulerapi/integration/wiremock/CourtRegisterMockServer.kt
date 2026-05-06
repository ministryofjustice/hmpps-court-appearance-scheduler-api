package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock

import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.havingExactly
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.courtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.WiremockConfig.mockServerConfig
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Court

class CourtRegisterMockServer : WireMockServer(mockServerConfig(9006)) {
  fun givenCourt(court: Court = court()): Court = givenCourts(setOf(court)).first()

  fun givenCourts(courts: Set<Court>, courtCodes: Set<String> = setOf()): Set<Court> {
    val codes = courtCodes.takeIf { it.isNotEmpty() } ?: courts.map { it.code }
    stubFor(
      get(urlPathEqualTo("/courts/id/multiple"))
        .withQueryParam("courtIds", havingExactly(*codes.toTypedArray()))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.OK.value())
            .withBody(
              jsonMapper().writeValueAsString(
                courts.map {
                  mapOf(
                    "courtId" to it.code,
                    "courtName" to it.name,
                  )
                },
              ),
            ),
        ),
    )
    return courts
  }

  companion object {
    fun court(code: String = courtCode(), name: String = word(10)) = Court(code, name)
  }
}

class CourterRegisterExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val courtRegister = CourtRegisterMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    courtRegister.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    courtRegister.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    courtRegister.stop()
  }
}
