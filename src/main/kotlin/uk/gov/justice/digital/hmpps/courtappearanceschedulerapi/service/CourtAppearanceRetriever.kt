package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.getAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.courtregister.CourtRegisterClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonregister.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Appearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Court
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Person
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Prison
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.asReason
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.asStatus
import java.util.*

@Service
class CourtAppearanceRetriever(
  private val appearanceRepository: CourtAppearanceRepository,
  private val prisonRegister: PrisonRegisterClient,
  private val courtRegister: CourtRegisterClient,
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
    id, person(), prison, court, reason.asReason(), external, start, end, comments, status.asStatus(),
  )

  private fun CourtAppearance.person(): Person = with(person) {
    Person(identifier, firstName, lastName, prisonCode, cellLocation)
  }
}
