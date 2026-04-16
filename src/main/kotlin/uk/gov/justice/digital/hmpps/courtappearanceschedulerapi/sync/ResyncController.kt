package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal.ResyncForPerson

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("resync")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class ResyncController(private val resync: ResyncForPerson) {
  @PutMapping("/court-appearances/{personIdentifier}")
  fun resyncCourtAppearances(
    @PathVariable personIdentifier: String,
    @RequestBody request: ResyncCourtEvents,
  ): ResyncResponse = resync.all(personIdentifier, request)
}
