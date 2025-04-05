package pl.ecommerce.customer.write.infrastructure.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import pl.ecommerce.commons.tracing.TraceService;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
	
	private final TraceService traceService;

	@ExceptionHandler(CustomerAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleCustomerAlreadyExists(CustomerAlreadyExistsException ex) {
		String traceId = traceService.getCurrentTraceId();
		log.error("Customer already exists: {}, traceId: {}", ex.getMessage(), traceId);

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"CUSTOMER_ALREADY_EXISTS",
						LocalDateTime.now(),
						"Try a different email or username.",
						traceId
				),
				HttpStatus.CONFLICT
		);
	}

	@ExceptionHandler(CustomerNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException ex) {
		String traceId = traceService.getCurrentTraceId();
		log.error("Customer not found: {}, traceId: {}", ex.getMessage(), traceId);

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"CUSTOMER_NOT_FOUND",
						LocalDateTime.now(),
						"Ensure the customer ID is correct.",
						traceId
				),
				HttpStatus.NOT_FOUND
		);
	}

	@ExceptionHandler(GdprConsentRequiredException.class)
	public ResponseEntity<ErrorResponse> handleGdprConsentRequired(GdprConsentRequiredException ex) {
		String traceId = traceService.getCurrentTraceId();
		log.error("GDPR consent required: {}, traceId: {}", ex.getMessage(), traceId);

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"GDPR_CONSENT_REQUIRED",
						LocalDateTime.now(),
						"User must provide consent before registration.",
						traceId
				),
				HttpStatus.FORBIDDEN
		);
	}

	@ExceptionHandler(CustomerNotActiveException.class)
	public ResponseEntity<ErrorResponse> handleCustomerNotActive(CustomerNotActiveException ex) {
		String traceId = traceService.getCurrentTraceId();
		log.error("Customer not active: {}, traceId: {}", ex.getMessage(), traceId);

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"CUSTOMER_NOT_ACTIVE",
						LocalDateTime.now(),
						"Customer must be active to perform this operation.",
						traceId
				),
				HttpStatus.FORBIDDEN
		);
	}

	@ExceptionHandler(AddressNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleAddressNotFound(AddressNotFoundException ex) {
		String traceId = traceService.getCurrentTraceId();
		log.error("Address not found: {}, traceId: {}", ex.getMessage(), traceId);

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"ADDRESS_NOT_FOUND",
						LocalDateTime.now(),
						"The specified address does not exist.",
						traceId
				),
				HttpStatus.NOT_FOUND
		);
	}

	@ExceptionHandler(CannotRemoveDefaultAddressException.class)
	public ResponseEntity<ErrorResponse> handleCannotRemoveDefaultAddress(CannotRemoveDefaultAddressException ex) {
		String traceId = traceService.getCurrentTraceId();
		log.error("Cannot remove default address: {}, traceId: {}", ex.getMessage(), traceId);

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"CANNOT_REMOVE_DEFAULT_ADDRESS",
						LocalDateTime.now(),
						"Set another address as default before removing this one.",
						traceId
				),
				HttpStatus.CONFLICT
		);
	}

	@ExceptionHandler(ConcurrencyException.class)
	public ResponseEntity<ErrorResponse> handleConcurrencyException(ConcurrencyException ex) {
		String traceId = traceService.getCurrentTraceId();
		log.error("Concurrency exception: {}, traceId: {}", ex.getMessage(), traceId);

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"CONCURRENCY_ERROR",
						LocalDateTime.now(),
						"The data was modified by another request. Please try again.",
						traceId
				),
				HttpStatus.CONFLICT
		);
	}

	@ExceptionHandler(EventStoreException.class)
	public ResponseEntity<ErrorResponse> handleEventStoreException(EventStoreException ex) {
		String traceId = traceService.getCurrentTraceId();
		log.error("Event store exception: {}, traceId: {}", ex.getMessage(), traceId);

		return new ResponseEntity<>(
				new ErrorResponse(
						"An error occurred while processing your request.",
						"EVENT_STORE_ERROR",
						LocalDateTime.now(),
						"Please try again later.",
						traceId
				),
				HttpStatus.INTERNAL_SERVER_ERROR
		);
	}

	@ExceptionHandler(WebExchangeBindException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(WebExchangeBindException ex) {
		String traceId = traceService.getCurrentTraceId();

		String validationErrors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));

		log.error("Validation error: {}, traceId: {}", validationErrors, traceId);

		return new ResponseEntity<>(
				new ErrorResponse(
						"Validation failed",
						"VALIDATION_ERROR",
						LocalDateTime.now(),
						validationErrors,
						traceId
				),
				HttpStatus.BAD_REQUEST
		);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
		String traceId = traceService.getCurrentTraceId();
		log.error("Unexpected error: {}, traceId: {}", ex.getMessage(), traceId, ex);

		return new ResponseEntity<>(
				new ErrorResponse(
						"An unexpected error occurred",
						"INTERNAL_SERVER_ERROR",
						LocalDateTime.now(),
						"Please contact support if the problem persists.",
						traceId
				),
				HttpStatus.INTERNAL_SERVER_ERROR
		);
	}
}