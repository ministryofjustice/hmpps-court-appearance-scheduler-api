package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal.SyncCourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal.SyncCourtMovement
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal.SyncRetriever
import java.util.UUID

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("sync")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class SyncController(
  private val appearance: SyncCourtAppearance,
  private val movement: SyncCourtMovement,
  private val retrieve: SyncRetriever,
) {
  @PutMapping("/court-appearances/{personIdentifier}")
  fun syncCourtAppearance(
    @PathVariable personIdentifier: String,
    @RequestBody request: SyncCourtEvent,
  ): ReferenceId = appearance.sync(personIdentifier, request)

  @PutMapping("/court-appearance-movements/{personIdentifier}")
  fun syncCourtAppearanceMovement(
    @PathVariable personIdentifier: String,
    @RequestBody request: SyncCourtEventMovement,
  ): ReferenceId = movement.sync(personIdentifier, request)

  @GetMapping("/court-appearances/{id}")
  fun getCourtAppearance(@PathVariable id: UUID): CourtEvent = retrieve.courtAppearance(id)

  @GetMapping("/court-appearance-movements/{id}")
  fun getCourtAppearanceMovement(@PathVariable id: UUID): CourtEventMovement = retrieve.courtAppearanceMovement(id)

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/court-appearances/{id}")
  fun deleteCourtAppearance(@PathVariable id: UUID) {
    if (!appearance.delete(id)) throw ConflictException("Cannot delete a scheduled appearance with a recorded movement.")
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/court-appearance-movements/{id}")
  fun deleteCourtAppearanceMovement(@PathVariable id: UUID) {
    movement.delete(id)
  }
}
