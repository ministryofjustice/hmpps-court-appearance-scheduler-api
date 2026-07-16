package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary.Companion.IDENTIFIER
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary.Companion.PRISON_CODE
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ExternalReference
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface CourtAppearanceRepository :
  JpaRepository<CourtAppearance, UUID>,
  JpaSpecificationExecutor<CourtAppearance> {
  fun findByLegacyId(legacyId: Long): CourtAppearance?
  fun findByExternalReference(externalReference: ExternalReference): CourtAppearance?
  fun countByPersonIdentifier(personIdentifier: String): Int

  @Query(
    """
    select ca.id from CourtAppearance ca
    where ca.person.identifier = :personIdentifier
  """,
  )
  fun findIdsForPersonIdentifier(personIdentifier: String): List<UUID>

  @Query(
    """
    select ca.id from CourtAppearance ca
    where ca.legacyId in :legacyIds
  """,
  )
  fun findIdsForLegacyIds(legacyIds: Set<Long>): List<UUID>

  fun findByStatusIdAndStartBefore(statusId: UUID, date: LocalDateTime): List<CourtAppearance>

  @EntityGraph(attributePaths = ["person", "reason", "status", "movements.reason"])
  fun findByPersonIdentifierAndExternalReferenceIsNotNull(personIdentifier: String): List<CourtAppearance>

  fun findByExternalReferenceIn(refs: Set<ExternalReference>): List<CourtAppearance>
}

fun CourtAppearanceRepository.getAppearance(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Court appearance not found")

fun appearanceMatchesCourtCodeIn(courtCodes: Set<String>) = Specification<CourtAppearance> { ca, _, _ ->
  ca.get<String>(CourtAppearance::courtCode.name).`in`(courtCodes)
}

fun appearanceMatchesPersonPrisonCode(prisonCode: String) = Specification<CourtAppearance> { ca, _, cb ->
  ca.join<CourtAppearance, PersonSummary>(CourtAppearance::person.name, JoinType.INNER)
    .matchesPrisonCode(cb, prisonCode)
}

fun appearanceMatchesPersonIdentifier(personIdentifier: String, prisonCode: String?) = Specification<CourtAppearance> { ca, _, cb ->
  val person = ca.join<CourtAppearance, PersonSummary>(CourtAppearance::person.name, JoinType.INNER)
  cb.and(
    person.matchesIdentifier(cb, personIdentifier),
    prisonCode?.let { person.matchesPrisonCode(cb, it) } ?: cb.conjunction(),
  )
}

fun appearanceMatchesPersonName(name: String, prisonCode: String?) = Specification<CourtAppearance> { ca, _, cb ->
  val person = ca.join<CourtAppearance, PersonSummary>(CourtAppearance::person.name, JoinType.INNER)
  cb.and(
    person.matchesName(cb, name),
    prisonCode?.let { person.matchesPrisonCode(cb, it) } ?: cb.conjunction(),
  )
}

fun appearancePersonIdentifierIn(identifiers: Set<String>, prisonCode: String) = Specification<CourtAppearance> { ca, _, cb ->
  val person = ca.join<CourtAppearance, PersonSummary>(CourtAppearance::person.name, JoinType.INNER)
  cb.and(person.get<String>(IDENTIFIER).`in`(identifiers), cb.equal(person.get<String>(PRISON_CODE), prisonCode))
}

fun appearanceStatusCodeIn(statusCodes: Set<CourtAppearanceStatus.Code>) = Specification<CourtAppearance> { ca, _, _ ->
  val status = ca.join<CourtAppearanceStatus, CourtAppearanceStatus>(CourtAppearance::status.name, JoinType.INNER)
  status.get<CourtAppearanceStatus.Code>(CourtAppearanceStatus::code.name).`in`(statusCodes)
}

fun appearanceNotUnscheduled() = Specification<CourtAppearance> { ca, _, cb ->
  val status = ca.join<CourtAppearanceStatus, CourtAppearanceStatus>(CourtAppearance::status.name, JoinType.INNER)
  cb.notEqual(status.get<CourtAppearanceStatus.Code>(CourtAppearanceStatus::code.name), CourtAppearanceStatus.Code.UNSCHEDULED)
}

fun appearanceReasonCodeIn(reasonCodes: Set<String>) = Specification<CourtAppearance> { ca, _, _ ->
  val status = ca.join<CourtAppearanceStatus, CourtAppearanceReason>(CourtAppearance::reason.name, JoinType.INNER)
  status.get<String>(CourtAppearanceReason::code.name).`in`(reasonCodes)
}

fun startsOnOrAfter(start: LocalDate) = startsAtOrAfter(start.atStartOfDay())

fun startsAtOrAfter(start: LocalDateTime) = Specification<CourtAppearance> { ca, _, cb ->
  cb.greaterThanOrEqualTo(ca.get(CourtAppearance::start.name), start)
}

fun startsOnOrBefore(end: LocalDate) = Specification<CourtAppearance> { ca, _, cb ->
  cb.lessThan(ca.get(CourtAppearance::start.name), end.plusDays(1).atStartOfDay())
}

fun appearanceMatchesExternal(external: Boolean) = Specification<CourtAppearance> { ca, _, cb ->
  cb.equal(ca.get<Boolean>(CourtAppearance::external.name), external)
}
