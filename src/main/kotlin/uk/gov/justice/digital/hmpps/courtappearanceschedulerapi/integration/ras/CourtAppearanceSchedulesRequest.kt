package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.ras

import java.util.UUID

data class CourtAppearanceSchedulesRequest(val uuids: Set<UUID>)
