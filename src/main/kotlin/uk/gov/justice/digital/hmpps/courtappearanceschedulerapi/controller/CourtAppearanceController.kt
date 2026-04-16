package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.controller

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.config.CaseloadIdHeader
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Appearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ScheduleCourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.CourtAppearanceAction
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.CourtAppearanceModifications
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.CourtAppearanceRetriever
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.CourtAppearanceScheduler
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.history.CourtAppearanceHistory
import java.util.UUID

@RestController
@RequestMapping("/court-appearances")
@PreAuthorize("hasAnyRole('${Roles.SCHEDULER_RO}', '${Roles.SCHEDULER_RW}', '${Roles.SCHEDULER_UI}')")
class CourtAppearanceController(
  private val retrieve: CourtAppearanceRetriever,
  private val history: CourtAppearanceHistory,
  private val schedule: CourtAppearanceScheduler,
  private val modify: CourtAppearanceModifications,
) {
  @CaseloadIdHeader
  @PreAuthorize("hasAnyRole('${Roles.SCHEDULER_RW}', '${Roles.SCHEDULER_UI}')")
  @PostMapping("/{personIdentifier}")
  fun scheduleCourtAppearance(
    @PathVariable personIdentifier: String,
    @RequestBody request: ScheduleCourtAppearance,
  ): ReferenceId = schedule.singleAppearance(personIdentifier, request)

  @GetMapping("/{id}")
  fun getCourtAppearance(@PathVariable id: UUID): Appearance = retrieve.byId(id)

  @CaseloadIdHeader
  @PreAuthorize("hasAnyRole('${Roles.SCHEDULER_RW}', '${Roles.SCHEDULER_UI}')")
  @PutMapping("/{id}")
  fun applyAction(@PathVariable id: UUID, @Valid @RequestBody request: CourtAppearanceAction): AuditHistory = modify.apply(id, request)

  @PreAuthorize("hasRole('${Roles.SCHEDULER_UI}')")
  @GetMapping("/{id}/history")
  fun getAppearanceHistory(@PathVariable id: UUID): AuditHistory = history.changes(id)
}
