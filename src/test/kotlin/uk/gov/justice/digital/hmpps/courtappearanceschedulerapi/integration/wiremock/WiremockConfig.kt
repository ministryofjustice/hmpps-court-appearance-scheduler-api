package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

object WiremockConfig {
  fun mockServerConfig(port: Int): WireMockConfiguration = wireMockConfig().port(port)
}

fun MappingBuilder.withBearerToken() = withHeader("Authorization", containing("Bearer "))
