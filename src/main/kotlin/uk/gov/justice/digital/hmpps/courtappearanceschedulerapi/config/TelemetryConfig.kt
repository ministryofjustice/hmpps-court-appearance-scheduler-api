package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TelemetryConfig {
  @Bean
  @ConditionalOnMissingBean
  fun telemetryClient(): TelemetryClient = TelemetryClient()
}
