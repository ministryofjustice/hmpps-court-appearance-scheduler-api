package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.ServiceConfig
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtProvider
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.appearanceMatchesCourtCodeIn
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.appearanceMatchesExternal
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.appearanceMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.appearanceMatchesPersonName
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.appearanceMatchesPersonPrisonCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.appearanceNotUnscheduled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.appearancePersonIdentifierIn
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.appearanceReasonCodeIn
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.appearanceStatusCodeIn
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.startsOnOrAfter
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.appearance.startsOnOrBefore
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.courtregister.CourtRegisterClient
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Court
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Person
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.asReason
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.asStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.AppearanceScheduleSearchRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.AppearanceSearchRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.CourtAppearanceResult
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.CourtAppearanceSchedule.Detail
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.CourtAppearanceSchedule.Detail.Companion.buildUiUrl
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.CourtAppearanceSchedules
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.CourtAppearanceSearchRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.CourtAppearanceSearchResponse
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.PageMetadata
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.PersonAppearanceSearchRequest

@Service
class SearchCourtAppearances(
  private val serviceConfig: ServiceConfig,
  private val appearanceRepository: CourtAppearanceRepository,
  private val courtRegister: CourtRegisterClient,
) {
  fun findForPrison(prisonCode: String, request: CourtAppearanceSearchRequest): CourtAppearanceSearchResponse = appearanceRepository.findAll(request.asSpecification(prisonCode), request.pageable()).asScheduleResponse()

  fun findForPerson(personIdentifier: String, request: PersonAppearanceSearchRequest): CourtAppearanceSearchResponse = appearanceRepository.findAll(request.asSpecification(personIdentifier), request.pageable()).asScheduleResponse()

  fun findSchedules(prisonCode: String, request: AppearanceScheduleSearchRequest): CourtAppearanceSchedules {
    val page = appearanceRepository.findAll(request.asSpecification(prisonCode), request.pageable())
    val courts = courtRegister.getCourts(page.map { it.courtCode }.toSet()).associateBy { it.code }
    return page.map { app -> app.asSchedule { courts[it] ?: Court.default(it) } }.asScheduleResponse()
  }

  private fun Page<CourtAppearance>.asScheduleResponse(): CourtAppearanceSearchResponse {
    val courts = courtRegister.getCourts(map { it.courtCode }.toSet()).associateBy { it.code }
    return map { item -> item.asResult { courts[it] ?: Court.default(it) } }.asResponse()
  }

  private fun AppearanceSearchRequest.defaults(): List<Specification<CourtAppearance>> = listOfNotNull(
    status.takeIf { it.isNotEmpty() }?.let { appearanceStatusCodeIn(it) } ?: appearanceNotUnscheduled(),
    reason.takeIf { it.isNotEmpty() }?.let { appearanceReasonCodeIn(it) },
    courtCodes.takeIf { it.isNotEmpty() }?.let { appearanceMatchesCourtCodeIn(it) },
    external?.let { appearanceMatchesExternal(it) },
  )

  private fun CourtAppearanceSearchRequest.asSpecification(prisonCode: String): Specification<CourtAppearance> = (
    listOfNotNull(
      startsOnOrAfter(start),
      startsOnOrBefore(end),
      query?.let {
        if (it.isPersonIdentifier()) {
          appearanceMatchesPersonIdentifier(it, prisonCode)
        } else {
          appearanceMatchesPersonName(it, prisonCode)
        }
      } ?: appearanceMatchesPersonPrisonCode(prisonCode),
    ) + defaults()
    ).reduce(Specification<CourtAppearance>::and)

  private fun PersonAppearanceSearchRequest.asSpecification(personIdentifier: String): Specification<CourtAppearance> = (
    listOfNotNull(
      appearanceMatchesPersonIdentifier(personIdentifier, null),
      start?.let { startsOnOrAfter(start) },
      end?.let { startsOnOrBefore(end) },
    ) + defaults()
    ).reduce(Specification<CourtAppearance>::and)

  private fun AppearanceScheduleSearchRequest.asSpecification(prisonCode: String): Specification<CourtAppearance> = (
    listOfNotNull(
      startsOnOrAfter(start),
      startsOnOrBefore(end),
      personIdentifiers.takeIf { it.isNotEmpty() }?.let { appearancePersonIdentifierIn(it, prisonCode) }
        ?: appearanceMatchesPersonPrisonCode(prisonCode),
    ) + defaults()
    ).reduce(Specification<CourtAppearance>::and)

  private fun String.isPersonIdentifier(): Boolean = matches(Prisoner.PATTERN.toRegex())

  private fun CourtAppearance.asResult(court: CourtProvider) = CourtAppearanceResult(
    id,
    person(),
    court(courtCode),
    reason.asReason(),
    external,
    start,
    end,
    comments,
    status.asStatus(),
  )

  private fun CourtAppearance.person(): Person = with(person) {
    Person(identifier, firstName, lastName, prisonCode, cellLocation)
  }

  private fun CourtAppearance.asSchedule(court: CourtProvider) = CourtAppearanceSchedule(
    id,
    person.identifier,
    court(courtCode),
    reason.asReason(),
    external,
    start,
    end,
    status.asStatus(),
    Detail(buildUiUrl(serviceConfig.uiBaseUrl, id), setOf()),
  )

  private fun Page<CourtAppearanceResult>.asResponse() = CourtAppearanceSearchResponse(content, PageMetadata(totalElements))

  private fun Page<CourtAppearanceSchedule>.asScheduleResponse() = CourtAppearanceSchedules(content, PageMetadata(totalElements))
}
