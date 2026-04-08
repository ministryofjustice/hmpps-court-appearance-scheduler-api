package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.exception.NotFoundException
import java.util.*

interface CourtAppearanceRepository : JpaRepository<CourtAppearance, UUID>

fun CourtAppearanceRepository.getAppearance(id: UUID) = findByIdOrNull(id) ?: throw NotFoundException("Court appearance not found")
