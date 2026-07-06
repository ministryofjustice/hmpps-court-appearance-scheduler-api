package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config

import jakarta.annotation.PostConstruct
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationUrlBuilder
import java.time.Duration

@Component
class ServiceConfigInfo(
  private val serviceConfig: ServiceConfig,
) : InfoContributor {
  override fun contribute(builder: Info.Builder) {
    builder.withDetail("activeAgencies", serviceConfig.activePrisons)
  }
}

@ConfigurationProperties(prefix = "service")
data class ServiceConfig(
  val activePrisons: Set<String>,
  val domainEvents: DomainEventConfig,
  val uiBaseUrl: String,
  val apiBaseUrl: String,
  val ras: RasConfig,
) {
  data class DomainEventConfig(val pollInterval: Duration, val batchSize: Int, val disabledEvents: Set<String> = setOf())
  data class RasConfig(val enableWithResync: Boolean, val enableWithSync: Boolean, val sendUpdates: Boolean)
}

@Component
class ServiceConfigLoader(private val serviceConfig: ServiceConfig) {
  @PostConstruct
  fun init() {
    IntegrationUrlBuilder.baseUrl = serviceConfig.apiBaseUrl
  }
}
