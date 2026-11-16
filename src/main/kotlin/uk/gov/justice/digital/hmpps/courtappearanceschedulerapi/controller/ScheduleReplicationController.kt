package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.OpenApiTags.REPLICATION
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.replication.IdSearchRequest
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.replication.IdSearchResponse
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.replication.FindIds
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@Tag(name = REPLICATION)
@RestController
@RequestMapping(value = ["/schedule-replication"])
class ScheduleReplicationController(private val findIds: FindIds) {
  private val concurrencyGate = Semaphore(1, true)

  @Operation(summary = "Finds the ids of scheduled court appearances for the provided parameters")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "400",
        description = "Bad request, client error. The response will contain information about the error.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "429",
        description = "Too many requests. Concurrent requests not permitted on this version, pages must be requested sequentially.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "200",
        description = "The response was successful.",
        content = [Content(schema = Schema(implementation = IdSearchResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('${Roles.SCHEDULE_REPLICATION_RO}')")
  @GetMapping("/court-appearance-ids")
  fun schedulerIdSearch(@Valid @ParameterObject request: IdSearchRequest): ResponseEntity<Any> {
    val acquired = try {
      concurrencyGate.tryAcquire(5, TimeUnit.SECONDS)
    } catch (_: InterruptedException) {
      Thread.currentThread().interrupt()
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
    }

    if (!acquired) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", "2")
        .body(
          ErrorResponse(
            HttpStatus.TOO_MANY_REQUESTS.value(),
            developerMessage = "Concurrent pagination limit reached. Fetch pages sequentially.",
          ),
        )
    }

    return try {
      ResponseEntity.ok(findIds.matching(request))
    } finally {
      concurrencyGate.release()
    }
  }
}
