package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.action.appearance

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain.CourtAppearance
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.CourtAppearanceRescheduled
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.events.domain.DomainEvent
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.StartAndEnd
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ValidStartAndEnd
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty0

@ValidReschedule
@ValidStartAndEnd
data class RescheduleAppearance(
  override val start: LocalDateTime?,
  override val end: LocalDateTime?,
) : CourtAppearanceAction,
  StartAndEnd<LocalDateTime> {
  override fun domainEvent(ca: CourtAppearance): DomainEvent<*> = CourtAppearanceRescheduled(ca.person.identifier, ca.id, ca.externalReference)
}

fun RescheduleAppearance.changes(
  startProperty: KMutableProperty0<LocalDateTime>,
  endProperty: KMutableProperty0<LocalDateTime?>,
): Boolean {
  val startChange = start != null && !start.truncatedTo(SECONDS).isEqual(startProperty.get().truncatedTo(SECONDS))
  val endChange = end != null &&
    (
      endProperty.get() == null ||
        !end.truncatedTo(SECONDS)
          .isEqual(checkNotNull(endProperty.get()).truncatedTo(SECONDS))
      )
  if (startChange) {
    startProperty.set(start.truncatedTo(SECONDS))
  }
  if (endChange) {
    endProperty.set(end.truncatedTo(SECONDS))
  }
  return startChange || endChange
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [RescheduleAppearanceValidator::class])
annotation class ValidReschedule(
  val message: String = DEFAULT_MESSAGE,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
) {
  companion object {
    const val DEFAULT_MESSAGE = "Either start or end must be provided."
  }
}

class RescheduleAppearanceValidator : ConstraintValidator<ValidReschedule, RescheduleAppearance> {
  override fun isValid(reschedule: RescheduleAppearance, context: ConstraintValidatorContext): Boolean = listOfNotNull(reschedule.start, reschedule.end).isNotEmpty()
}
