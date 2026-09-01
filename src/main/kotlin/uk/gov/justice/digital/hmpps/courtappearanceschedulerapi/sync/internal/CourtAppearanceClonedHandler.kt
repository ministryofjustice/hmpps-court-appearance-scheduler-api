package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.externalreference.ExternalReference
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.externalreference.ExternalReferenceEntity
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.externalreference.ExternalReferenceService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceCloned
import java.util.UUID

@Transactional
@Service
class CourtAppearanceClonedHandler(private val car: CourtAppearanceRepository) {
  fun handle(event: CourtAppearanceCloned) {
    val previousEr = event.additionalInformation.previousId.asExternalReference()
    val clonedEr = event.additionalInformation.clonedId.asExternalReference()

    val found = car.findByExternalReferenceIn(setOf(previousEr, clonedEr)).associateBy { it.externalReference }
    check(found.size == 2) { "Both appearances must exist to process a clone event" }

    found[previousEr]?.also { it.applyExternalIdentifiers(clonedEr, it.legacyId) }
    found[clonedEr]?.also { it.applyExternalIdentifiers(previousEr, it.legacyId) }
  }

  private fun UUID.asExternalReference(): ExternalReference = ExternalReference(ExternalReferenceService.REMAND_AND_SENTENCING, ExternalReferenceEntity.COURT_APPEARANCE, this)
}
