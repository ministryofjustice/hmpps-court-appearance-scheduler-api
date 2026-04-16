package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.sync.internal.MoveToAnotherPerson
import java.util.UUID

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("move")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class MoveController(private val move: MoveToAnotherPerson) {
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PutMapping("court-appearances")
  fun moveCourtAppearances(@RequestBody request: MoveCourtEventRequest) = move.appearancesAndMovements(request)
}

data class MoveCourtEventRequest(
  val fromPersonIdentifier: String,
  val toPersonIdentifier: String,
  val scheduleIds: Set<UUID>,
  val unscheduledMovementIds: Set<UUID>,
)
