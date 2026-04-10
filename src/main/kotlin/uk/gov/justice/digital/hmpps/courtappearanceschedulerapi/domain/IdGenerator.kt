package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import com.fasterxml.uuid.Generators
import java.util.*

object IdGenerator {
  fun newUuid(): UUID = Generators.timeBasedEpochGenerator().generate()
}

interface Identifiable {
  val id: UUID
  val version: Int?
}
