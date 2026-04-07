package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CourtAppearanceRepository : JpaRepository<CourtAppearance, UUID>
