package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.ExternalReferenceService
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.courtregister.CourtRegisterClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonregister.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras.RemandAndSentencingClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Appearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Court
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Person
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Prison
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.asAppearanceOrigin
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.asReason
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.asStatus
import java.time.LocalDateTime
import java.util.UUID

@Service
class CourtAppearanceRetriever(
  private val appearanceRepository: CourtAppearanceRepository,
  private val prisonRegister: PrisonRegisterClient,
  private val courtRegister: CourtRegisterClient,
  private val rasClient: RemandAndSentencingClient,
) {
  fun byId(id: UUID): Appearance {
    val appearance = appearanceRepository.getAppearance(id)
    val (prison, court) = Mono.zip(
      prisonRegister.findPrison(appearance.prisonCode),
      courtRegister.findCourt(appearance.courtCode),
    ).map { it.t1 to it.t2 }.block()!!
    return appearance.toModel(prison, court)
  }

  private fun CourtAppearance.toModel(prison: Prison, court: Court) = Appearance(
    id,
    person(),
    prison,
    court,
    reason.asReason(),
    external,
    start,
    end,
    comments,
    status.asStatus(),
    externalReference?.asAppearanceOrigin(),
    isCancellable(),
  )

  private fun CourtAppearance.isCancellable(): Boolean {
    val rasReference = externalReference?.takeIf { it.service == ExternalReferenceService.REMAND_AND_SENTENCING }
    return (status.code == CourtAppearanceStatus.Code.SCHEDULED && rasReference == null) ||
      rasReference?.uuid?.let { start.isAfter(LocalDateTime.now()) && rasClient.canDeleteAppearance(it) } == true
  }

  private fun CourtAppearance.person(): Person = with(person) {
    Person(identifier, firstName, lastName, prisonCode, cellLocation)
  }
}
