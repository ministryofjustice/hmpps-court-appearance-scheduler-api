package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.person

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.PrisonerReceived
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.PrisonerReceivedInformation.Companion.BOOKING_SWITCHED_REASON
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.nomis.MigrationClient

@Service
class PrisonerReceivedHandler(private val migrationClient: MigrationClient) {
  fun handle(pre: PrisonerReceived) {
    if (pre.additionalInformation.reason != BOOKING_SWITCHED_REASON) return
    migrationClient.requestRepair(pre.additionalInformation.nomsNumber)
  }
}
