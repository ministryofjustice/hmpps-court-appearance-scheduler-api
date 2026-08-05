package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.access

object Roles {
  const val SCHEDULER_UI = "ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER_UI"
  const val SCHEDULER_RO = "ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RO"
  const val SCHEDULER_RW = "ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RW"
  const val SCHEDULE_CLASHES_RO = "ROLE_SCHEDULES__CLASHES__RO"
  const val NOMIS_SYNC = "ROLE_COURT_APPEARANCES__SYNC__RW"

  fun allExcept(vararg except: String): List<String> = listOf(
    SCHEDULER_UI,
    SCHEDULER_RO,
    SCHEDULER_RW,
    SCHEDULE_CLASHES_RO,
    NOMIS_SYNC,
  ).filter { it !in except }
}
