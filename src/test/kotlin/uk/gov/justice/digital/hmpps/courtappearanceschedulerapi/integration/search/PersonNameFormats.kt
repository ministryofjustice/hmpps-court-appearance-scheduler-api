package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.search

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary

fun PersonSummary.nameFormats(): List<String> = listOf(
  "$firstName $lastName",
  "$lastName $firstName",
  "$lastName,$firstName",
  "$lastName, $firstName",
)
