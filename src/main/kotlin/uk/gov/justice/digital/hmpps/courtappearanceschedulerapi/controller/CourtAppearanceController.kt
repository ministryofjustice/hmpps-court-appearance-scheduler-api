package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Appearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.RetrieveCourtAppearance
import java.util.*

@RestController
@RequestMapping("/court-appearances")
@PreAuthorize("hasAnyRole('${Roles.SCHEDULER_RO}', '${Roles.SCHEDULER_RW}', '${Roles.SCHEDULER_UI}')")
class CourtAppearanceController(
  private val retrieve: RetrieveCourtAppearance,
) {
  @GetMapping("/{id}")
  fun getCourtAppearance(@PathVariable id: UUID): Appearance = retrieve.byId(id)
}
