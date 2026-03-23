package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CourtAppearanceSchedulerApi

fun main(args: Array<String>) {
  runApplication<CourtAppearanceSchedulerApi>(*args)
}
