package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.CourtAppearanceReasons
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.RetrieveReasons

@RestController
@RequestMapping("/court-appearance-reasons")
@PreAuthorize("hasAnyRole('${Roles.SCHEDULER_RO}', '${Roles.SCHEDULER_RW}', '${Roles.SCHEDULER_UI}')")
class CourtAppearanceReasonController(private val retrieve: RetrieveReasons) {
  @GetMapping
  fun getCourtAppearanceReasons(): CourtAppearanceReasons = retrieve.allReasons()
}
