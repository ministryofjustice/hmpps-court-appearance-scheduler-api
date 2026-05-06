package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config

import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import reactor.netty.http.HttpProtocol
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
  val httpProtocol: HttpProtocol,
) {
  data class DomainEventConfig(val pollInterval: Duration, val batchSize: Int)
}
