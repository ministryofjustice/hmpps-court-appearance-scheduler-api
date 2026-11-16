package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.replication

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.appearanceMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.appearanceMatchesPersonPrisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.startsOnOrAfter
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.startsOnOrBefore
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.PageMetadata
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.replication.IdResponse
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.replication.IdSearchRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.replication.IdSearchResponse

@Service
class FindIds(private val caRepository: CourtAppearanceRepository) {
  fun matching(request: IdSearchRequest): IdSearchResponse {
    val ids = caRepository.findIds(request.asSpecification(), request.pageable()).map { IdResponse(it) }
    return IdSearchResponse(ids.content, PageMetadata(ids.totalElements))
  }

  private fun IdSearchRequest.asSpecification() = listOfNotNull(
    startsOnOrAfter(start),
    startsOnOrBefore(end),
    personIdentifier?.let { appearanceMatchesPersonIdentifier(personIdentifier, prisonCode) }
      ?: prisonCode?.let { appearanceMatchesPersonPrisonCode(prisonCode) },
  ).reduce(Specification<CourtAppearance>::and)
}
