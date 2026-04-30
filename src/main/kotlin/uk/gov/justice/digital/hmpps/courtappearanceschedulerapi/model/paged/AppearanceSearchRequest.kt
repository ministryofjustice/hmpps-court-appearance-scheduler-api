package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.by
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearanceStatus
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary.Companion.FIRST_NAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary.Companion.IDENTIFIER
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.PersonSummary.Companion.LAST_NAME
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.StartAndEnd
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.DAYS
import kotlin.properties.Delegates
import kotlin.reflect.KClass

interface AppearanceSearchRequest :
  PagedRequest,
  StartAndEnd<LocalDate> {
  val status: Set<CourtAppearanceStatus.Code>
  val reason: Set<String>
  val courtCodes: Set<String>
  val external: Boolean?

  override fun validSortFields(): Set<String> = setOf(START, END, STATUS, FIRST_NAME, LAST_NAME, REASON)

  override fun buildSort(field: String, direction: Direction): Sort = when (field) {
    LAST_NAME -> sortByPersonName(direction)
    FIRST_NAME -> sortByPersonName(direction, PERSON_FIRST_NAME, PERSON_LAST_NAME)
    START -> sortByDate(direction, START, END)
    END -> sortByDate(direction, END, START)
    STATUS -> by(direction, "${field}_${SEQUENCE_NUMBER}").and(sortByPersonName())
    REASON -> by(direction, "${field}_description").and(sortByPersonName())

    else -> throw IllegalArgumentException("Unrecognised sort field")
  }

  private fun sortByDate(direction: Direction, first: String, second: String) = by(direction, first, second).and(sortByPersonName())

  private fun sortByPersonName(
    direction: Direction = Direction.ASC,
    first: String = PERSON_LAST_NAME,
    second: String = PERSON_FIRST_NAME,
  ) = by(
    direction,
    first,
    second,
    "${PERSON}_$IDENTIFIER",
  )

  companion object {
    private val START = CourtAppearance::start.name
    private val END = CourtAppearance::end.name
    private val REASON = CourtAppearance::reason.name
    private val STATUS = CourtAppearance::status.name
    private val SEQUENCE_NUMBER = CourtAppearanceStatus::sequenceNumber.name
    private val PERSON = CourtAppearance::person.name
    private val PERSON_LAST_NAME = "${PERSON}_${LAST_NAME}"
    private val PERSON_FIRST_NAME = "${PERSON}_${FIRST_NAME}"
  }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [MonthBetweenValidator::class])
annotation class ValidDateRange(
  val daysBetween: Int = 31,
  val message: String = "Invalid date range",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
)

class MonthBetweenValidator : ConstraintValidator<ValidDateRange, StartAndEnd<*>> {
  private var daysBetween by Delegates.notNull<Int>()

  override fun initialize(constraintAnnotation: ValidDateRange) {
    daysBetween = constraintAnnotation.daysBetween
  }

  override fun isValid(request: StartAndEnd<*>, context: ConstraintValidatorContext): Boolean = with(request) {
    return if (start == null || end == null) {
      false
    } else {
      when (start) {
        is LocalDate -> DAYS.between(start as LocalDate, end as LocalDate) <= daysBetween
        is LocalDateTime -> Duration.between(start, end).toDays() <= daysBetween
        else -> throw UnsupportedOperationException("${start!!::class.simpleName} is not supported by this validator")
      }
    }
  }
}
