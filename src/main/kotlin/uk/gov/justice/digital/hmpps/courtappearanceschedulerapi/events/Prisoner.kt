package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events

data class PrisonerUpdatedInformation(
  val nomsNumber: String,
  val categoriesChanged: Set<String>,
) : AdditionalInformation {
  companion object {
    val CATEGORIES_OF_INTEREST = setOf("PERSONAL_DETAILS", "LOCATION")
  }
}

data class PrisonerUpdated(
  override val additionalInformation: PrisonerUpdatedInformation,
  override val personReference: PersonReference,
) : DomainEvent<PrisonerUpdatedInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "prisoner-offender-search.prisoner.updated"
    const val DESCRIPTION: String = "Detail about the prisoner have changed"
  }
}

data class PrisonerMergedInformation(val removedNomsNumber: String, val nomsNumber: String) : AdditionalInformation

data class PrisonerMerged(
  override val additionalInformation: PrisonerMergedInformation,
  override val personReference: PersonReference,
) : DomainEvent<PrisonerMergedInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "prison-offender-events.prisoner.merged"
    const val DESCRIPTION: String = "Prisoner records merged"
  }
}
