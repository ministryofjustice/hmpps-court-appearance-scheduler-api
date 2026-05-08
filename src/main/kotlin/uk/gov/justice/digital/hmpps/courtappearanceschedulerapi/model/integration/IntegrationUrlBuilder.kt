package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration

import java.util.UUID

object IntegrationUrlBuilder {
  lateinit var baseUrl: String

  fun appearanceUrl(id: UUID): String = "$baseUrl/integrations/court-appearances/$id"

  fun appearanceMovementsUrl(id: UUID): String = "$baseUrl/integrations/court-appearances/$id/movements"

  fun movementUrl(id: UUID): String = "$baseUrl/integrations/court-appearance-movements/$id"
}
