package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.service.court

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtCode
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtCodeRepository
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtCodePull
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.CourtCodePull.Companion.ALL
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.internal.InternalEventEmitter
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.courtregister.CourtRegisterClient

@Service
@Transactional
class CourtCodeService(
  private val iee: InternalEventEmitter,
  private val courtRegister: CourtRegisterClient,
  private val courtCodeRepository: CourtCodeRepository,
) {
  fun getCourtCode(courtCode: String): CourtCode {
    val court = courtRegister.getCourt(courtCode)
    return courtCodeRepository.findByIdOrNull(court.code)
      ?.also { if (court.name != court.code) it.applySort(court.name) }
      ?: courtCodeRepository.save(CourtCode(court.code, court.name))
  }

  fun getCourtCodes(courtCodes: Set<String>): List<CourtCode> {
    val courts = courtRegister.getCourts(courtCodes).associateBy { it.code }
    val sorts = courtCodeRepository.findAllById(courts.keys).associateBy { it.code }
    return courtCodeRepository.saveAll(
      courtCodes.map { code ->
        val court = courts[code]
        sorts[code]?.also { if (court?.name != court?.code) it.applySort(code) }
          ?: courtCodeRepository.save(CourtCode(code, court?.name ?: code))
      },
    )
  }

  fun handleEvent(event: CourtCodePull) = when (event.code) {
    ALL -> courtCodeRepository.findAll().forEach { iee.publishInternalEvent(CourtCodePull(it.code)) }
    else -> getCourtCode(event.code)
  }
}
