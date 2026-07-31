package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.clashesFor
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.courtregister.CourtRegisterClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Court
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes.Clash
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes.ClashOrigin
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes.ClashPersonIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes.ClashRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes.ClashResponse
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes.ClashSource
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.clashes.PersonClashes

@Transactional(readOnly = true)
@Service
class RetrieveClashes(
  private val appearanceRepository: CourtAppearanceRepository,
  private val courtRegisterClient: CourtRegisterClient,
) {
  fun retrieve(request: ClashRequest): ClashResponse {
    val appearances = appearanceRepository.findAll(
      clashesFor(request.personIdentifiers.map { it.value }.toSet(), request.ranges),
    )
    val courts = courtRegisterClient.getCourts(appearances.map { it.courtCode }.toSet()).associateBy { it.code }
    val courtSupplier = { code: String -> courts[code] ?: Court.default(code) }

    return appearances.groupBy { it.person.identifier }
      .map { (k, v) -> PersonClashes(k.asPersonIdentifier(), v.map { it.clash(courtSupplier) }) }
      .let { ClashResponse(ORIGIN, it) }
  }

  private fun CourtAppearance.clash(courtSupplier: (String) -> Court): Clash {
    val court = courtSupplier(courtCode)
    return Clash(
      start,
      end,
      description(),
      Clash.Location(court.name),
      Clash.AdditionalInformation(court.code),
    )
  }

  private fun CourtAppearance.description() = Clash.Description(reason.description, if (external) "Court appearance" else "Video link")

  private fun String.asPersonIdentifier() = ClashPersonIdentifier(ClashPersonIdentifier.Type.PRISON_NUMBER, this)

  companion object {
    const val PRODUCT_ID = "DPS135"
    val ORIGIN: ClashOrigin = ClashOrigin(ClashSource(PRODUCT_ID, "Schedule a court appearance"))
  }
}
