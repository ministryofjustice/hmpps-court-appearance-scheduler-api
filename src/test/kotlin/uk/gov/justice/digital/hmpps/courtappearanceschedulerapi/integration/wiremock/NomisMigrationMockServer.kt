package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonersearch.Prisoner

class NomisMigrationServer : WireMockServer(9990) {
  fun requestRepair(status: HttpStatus = HttpStatus.OK) {
    stubFor(
      put(urlMatching("/migrate/court-appearance/repair/${Prisoner.PATTERN}"))
        .willReturn(aResponse().withStatus(status.value())),
    )
  }
}

class NomisMigrationExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val nomisMigration = NomisMigrationServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    nomisMigration.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    nomisMigration.resetRequests()
    nomisMigration.requestRepair()
  }

  override fun afterAll(context: ExtensionContext) {
    nomisMigration.stop()
  }
}
