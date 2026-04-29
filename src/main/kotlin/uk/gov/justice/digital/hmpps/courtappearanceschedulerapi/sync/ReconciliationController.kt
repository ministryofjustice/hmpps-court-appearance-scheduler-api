package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal.SyncRetriever

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("reconciliation")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class ReconciliationController(private val retrieve: SyncRetriever) {
  @GetMapping("/court-appearances/{personIdentifier}")
  fun getCourtAppearances(@PathVariable personIdentifier: String): ReconciliationResponse = retrieve.allFor(personIdentifier)
}
