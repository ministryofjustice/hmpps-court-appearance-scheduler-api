package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Court

typealias CourtProvider = (String) -> Court
typealias ReasonProvider = (String) -> CourtAppearanceReason
typealias StatusProvider = (CourtAppearanceStatus.Code) -> CourtAppearanceStatus
