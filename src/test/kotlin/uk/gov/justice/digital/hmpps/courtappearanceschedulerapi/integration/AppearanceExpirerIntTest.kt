package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.CourtAppearanceExpired
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.config.CourtAppearanceOperations.Companion.courtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance.RescheduleAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.AppearanceExpirer
import java.time.LocalDate

class AppearanceExpirerIntTest(
  @Autowired cao: CourtAppearanceOperations,
  @Autowired private val appearanceRepository: CourtAppearanceRepository,
  @Autowired private val expirer: AppearanceExpirer,
) : IntegrationTest(),
  CourtAppearanceOperations by cao {
  @Test
  fun `expires past scheduled appearances`() {
    val start = LocalDate.now().minusDays(1).atTime(10, 0)
    val end = LocalDate.now().minusDays(1).atTime(17, 0)
    val scheduled = (0..2).map { givenCourtAppearance(courtAppearance()) }
    scheduled.forEach { appearanceRepository.save(it.reschedule(RescheduleAppearance(start, end))) }
    val toExpire = appearanceRepository.findAllById(scheduled.map { it.id }.toSet())
    toExpire.forEach { assertThat(it.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED) }
    val noExpire = givenCourtAppearance(courtAppearance(start = start.plusDays(2), end = end.plusDays(2)))

    expirer.expireScheduledAppearances()

    val expired = appearanceRepository.findAllById(toExpire.map { it.id }.toSet())
    expired.forEach { assertThat(it.status.code).isEqualTo(CourtAppearanceStatus.Code.EXPIRED) }
    val notExpired = requireNotNull(findCourtAppearance(noExpire.id))
    assertThat(notExpired.status.code).isEqualTo(CourtAppearanceStatus.Code.SCHEDULED)

    verifyEventPublications(
      scheduled.first(),
      scheduled.map {
        CourtAppearanceExpired(it.person.identifier, it.id, it.externalReference).publication(it.id)
      }.toSet(),
    )
  }
}
