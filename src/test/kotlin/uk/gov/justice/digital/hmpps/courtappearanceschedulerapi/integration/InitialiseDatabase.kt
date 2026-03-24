package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.junit.jupiter.api.Test

class InitialiseDatabase : IntegrationTestBase() {

  @Test
  fun `initialises database`() {
    println("Database has been initialised by IntegrationTestBase")
  }
}
