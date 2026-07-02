package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.http.HttpStatus
import tools.jackson.module.kotlin.jsonMapper
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonapi.PrisonerMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock.WiremockConfig.mockServerConfig
import java.time.LocalDateTime

class PrisonApiMockServer : WireMockServer(mockServerConfig(9004)) {
  fun givenMovementsFor(personIdentifier: String, movements: List<PrisonerMovement>): List<PrisonerMovement> {
    stubFor(
      get(urlPathEqualTo("/api/movements/offender/$personIdentifier"))
        .withBearerToken()
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.OK.value())
            .withBody(jsonMapper().writeValueAsString(movements)),
        ),
    )
    return movements
  }

  companion object {
    fun prisonerMovement(
      toAgency: String = prisonCode(),
      movementType: String = PrisonerMovement.ADMISSION_TYPE,
      dateTime: LocalDateTime = LocalDateTime.now(),
    ) = PrisonerMovement(toAgency, movementType, dateTime.toLocalDate(), dateTime.toLocalTime())
  }
}

class PrisonerApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonApi = PrisonApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonApi.stop()
  }
}
