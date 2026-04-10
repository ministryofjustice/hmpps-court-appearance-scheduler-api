package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.controller

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.CourtAppearanceSearchRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.CourtAppearanceSearchResponse
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.PersonAppearanceSearchRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.SearchCourtAppearances

@RestController
@RequestMapping("/search")
@PreAuthorize("hasRole('${Roles.SCHEDULER_UI}')")
class SearchController(
  private val appearance: SearchCourtAppearances,
) {
  @PostMapping("/prisons/{prisonCode}/court-appearances")
  fun findAppearancesForPrison(
    @PathVariable prisonCode: String,
    @Valid @RequestBody request: CourtAppearanceSearchRequest,
  ): CourtAppearanceSearchResponse = appearance.findForPrison(prisonCode, request)

  @PostMapping("/people/{personIdentifier}/court-appearances")
  fun findAppearancesForPerson(
    @PathVariable personIdentifier: String,
    @Valid @RequestBody request: PersonAppearanceSearchRequest,
  ): CourtAppearanceSearchResponse = appearance.findForPerson(personIdentifier, request)
}
