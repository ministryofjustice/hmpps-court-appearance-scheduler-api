package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "SyncUser")
data class User(val username: String, val activeCaseloadId: String?)
