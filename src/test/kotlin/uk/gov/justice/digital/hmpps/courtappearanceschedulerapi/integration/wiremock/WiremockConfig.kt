package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

object WiremockConfig {
  fun mockServerConfig(port: Int): WireMockConfiguration = wireMockConfig().port(port)
}
