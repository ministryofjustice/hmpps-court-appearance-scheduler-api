package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Court
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.Prison

typealias CourtProvider = (String) -> Court
typealias PrisonProvider = (String) -> Prison
typealias ReasonProvider = (String) -> CourtAppearanceReason
typealias StatusProvider = (CourtAppearanceStatus.Code) -> CourtAppearanceStatus
