package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.ServiceConfig

@EnableScheduling
@EnableConfigurationProperties(ServiceConfig::class)
@SpringBootApplication
class CourtAppearanceSchedulerApi

fun main(args: Array<String>) {
  runApplication<CourtAppearanceSchedulerApi>(*args)
}
