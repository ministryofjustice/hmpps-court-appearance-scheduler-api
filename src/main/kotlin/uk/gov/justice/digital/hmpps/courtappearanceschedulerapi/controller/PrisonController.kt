package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.PrisonOverview
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.OverviewRetriever

@RestController
@RequestMapping("/prisons")
@PreAuthorize("hasRole('${Roles.SCHEDULER_UI}')")
class PrisonController(private val overview: OverviewRetriever) {
  @GetMapping("/{prisonCode}/external-movements/overview")
  fun getPrisonOverview(@PathVariable prisonCode: String): PrisonOverview = overview.forPrison(prisonCode)
}
