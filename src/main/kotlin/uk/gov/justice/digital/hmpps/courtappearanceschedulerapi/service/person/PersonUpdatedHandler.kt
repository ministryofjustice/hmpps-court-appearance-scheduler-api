package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.PrisonerUpdatedInformation
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.PrisonerUpdatedInformation.Companion.CATEGORIES_OF_INTEREST

@Service
class PersonUpdatedHandler(private val personSummaryService: PersonSummaryService) {
  fun handle(de: DomainEvent<PrisonerUpdatedInformation>) {
    val matchingChanges = de.additionalInformation.categoriesChanged intersect CATEGORIES_OF_INTEREST
    if (matchingChanges.isNotEmpty()) {
      personSummaryService.updateExistingDetails(de.getPersonIdentifier())
    }
  }
}
