package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.OpenApiTags.UI
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.CourtAppearanceReasons
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.ReasonRetriever

@Tag(name = UI)
@RestController
@RequestMapping("/court-appearance-reasons")
@PreAuthorize("hasRole('${Roles.SCHEDULER_UI}')")
class CourtAppearanceReasonController(private val retrieve: ReasonRetriever) {
  @GetMapping
  fun getCourtAppearanceReasons(): CourtAppearanceReasons = retrieve.allReasons()
}
