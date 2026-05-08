package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.OpenApiTags.INTEGRATIONS
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationResponse
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationResponses
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationUrlBuilder.appearanceMovementsUrl
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.integration.IntegrationUrlBuilder.appearanceUrl
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.IntegrationRetriever
import java.util.UUID

@Tag(name = INTEGRATIONS)
@RestController
@RequestMapping(value = ["integrations"])
@PreAuthorize("hasAnyRole('${Roles.SCHEDULER_RO}', '${Roles.SCHEDULER_RW}')")
class IntegrationController(private val retrieve: IntegrationRetriever) {
  @GetMapping("/court-appearances/{id}")
  fun appearance(@PathVariable id: UUID): IntegrationResponse<IntegrationAppearance> = IntegrationResponse(retrieve.appearance(id), null, appearanceMovementsUrl(id))

  @GetMapping("/court-appearances/{id}/movements")
  fun appearanceMovements(@PathVariable id: UUID): IntegrationResponses<IntegrationMovement> = retrieve.movementsForAppearance(id)
    .map { IntegrationResponse(it, appearanceUrl(id), null) }
    .let { IntegrationResponses(it, appearanceUrl(id)) }

  @GetMapping("/court-appearance-movements/{id}")
  fun movement(@PathVariable id: UUID): IntegrationResponse<IntegrationMovement> {
    val movement = retrieve.movement(id)
    return IntegrationResponse(movement, movement.scheduleId?.let { appearanceUrl(it) }, null)
  }
}
