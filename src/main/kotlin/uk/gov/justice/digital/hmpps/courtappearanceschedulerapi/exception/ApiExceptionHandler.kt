package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.exception

import jakarta.validation.ValidationException
import org.springframework.context.MessageSourceResolvable
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestControllerAdvice
class ApiExceptionHandler {
  @ExceptionHandler(ConflictException::class)
  fun handleConflictException(e: ConflictException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(CONFLICT)
    .body(
      ErrorResponse(
        status = CONFLICT,
        userMessage = e.message,
        developerMessage = e.devMessage(),
      ),
    )

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.devMessage(),
      ),
    )

  @ExceptionHandler(
    IllegalArgumentException::class,
    IllegalStateException::class,
    HttpMessageNotReadableException::class,
  )
  fun handleGenericBadRequests(e: RuntimeException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Invalid request: ${e.message}",
        developerMessage = e.devMessage(),
      ),
    )

  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<ErrorResponse> = e.allErrors.mapErrors()

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> = e.allErrors.mapErrors()

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(FORBIDDEN)
    .body(
      ErrorResponse(
        status = FORBIDDEN,
        userMessage = "Forbidden: ${e.message}",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(NotFoundException::class)
  fun handleNoResourceFoundException(e: NotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = e.message,
        developerMessage = e.devMessage(),
      ),
    )

  @ExceptionHandler(DataIntegrityViolationException::class, DataAccessException::class)
  fun handleConflictException(e: RuntimeException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(CONFLICT)
    .body(
      ErrorResponse(
        status = CONFLICT,
        userMessage = "Data integrity conflict",
        developerMessage = e.devMessage(),
      ),
    )

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    )
}

private fun RuntimeException.devMessage(): String = message ?: "${this::class.simpleName}: ${cause?.message ?: ""}"

private fun List<MessageSourceResolvable>.mapErrors() = map { it.defaultMessage!! }.distinct().sorted().let {
  val validationFailure = "Validation failure"
  val message = if (it.size > 1) {
    """
    |${validationFailure}s: 
    |${it.joinToString(System.lineSeparator())}
    |
    """.trimMargin()
  } else {
    "$validationFailure: ${it.joinToString(System.lineSeparator())}"
  }
  ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = message,
        developerMessage = "400 BAD_REQUEST $message",
      ),
    )
}
