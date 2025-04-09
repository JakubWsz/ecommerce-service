package pl.ecommerce.customer.write.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomerAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleCustomerAlreadyExists(CustomerAlreadyExistsException ex) {
		log.error("Customer already exists: {}", ex.getMessage());

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"CUSTOMER_ALREADY_EXISTS",
						LocalDateTime.now(),
						"Try a different email or username."
				),
				HttpStatus.CONFLICT
		);
	}

	@ExceptionHandler(CustomerNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException ex) {
		log.error("Customer not found: {}", ex.getMessage());

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"CUSTOMER_NOT_FOUND",
						LocalDateTime.now(),
						"Ensure the customer ID is correct."
				),
				HttpStatus.NOT_FOUND
		);
	}

	@ExceptionHandler(GdprConsentRequiredException.class)
	public ResponseEntity<ErrorResponse> handleGdprConsentRequired(GdprConsentRequiredException ex) {
		log.error("GDPR consent required: {}", ex.getMessage());

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"GDPR_CONSENT_REQUIRED",
						LocalDateTime.now(),
						"User must provide consent before registration."
				),
				HttpStatus.FORBIDDEN
		);
	}

	@ExceptionHandler(CustomerNotActiveException.class)
	public ResponseEntity<ErrorResponse> handleCustomerNotActive(CustomerNotActiveException ex) {
		log.error("Customer not active: {}", ex.getMessage());

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"CUSTOMER_NOT_ACTIVE",
						LocalDateTime.now(),
						"Customer must be active to perform this operation."
				),
				HttpStatus.FORBIDDEN
		);
	}

	@ExceptionHandler(AddressNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleAddressNotFound(AddressNotFoundException ex) {
		log.error("Address not found: {}", ex.getMessage());

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"ADDRESS_NOT_FOUND",
						LocalDateTime.now(),
						"The specified address does not exist."
				),
				HttpStatus.NOT_FOUND
		);
	}

	@ExceptionHandler(CannotRemoveDefaultAddressException.class)
	public ResponseEntity<ErrorResponse> handleCannotRemoveDefaultAddress(CannotRemoveDefaultAddressException ex) {
		log.error("Cannot remove default address: {}", ex.getMessage());

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"CANNOT_REMOVE_DEFAULT_ADDRESS",
						LocalDateTime.now(),
						"Set another address as default before removing this one."
				),
				HttpStatus.CONFLICT
		);
	}

	@ExceptionHandler(ConcurrencyException.class)
	public ResponseEntity<ErrorResponse> handleConcurrencyException(ConcurrencyException ex) {
		log.error("Concurrency exception: {}", ex.getMessage());

		return new ResponseEntity<>(
				new ErrorResponse(
						ex.getMessage(),
						"CONCURRENCY_ERROR",
						LocalDateTime.now(),
						"The data was modified by another request. Please try again."
				),
				HttpStatus.CONFLICT
		);
	}

	@ExceptionHandler(EventStoreException.class)
	public ResponseEntity<ErrorResponse> handleEventStoreException(EventStoreException ex) {
		log.error("Event store exception: {}", ex.getMessage());

		return new ResponseEntity<>(
				new ErrorResponse(
						"An error occurred while processing your request.",
						"EVENT_STORE_ERROR",
						LocalDateTime.now(),
						"Please try again later."
				),
				HttpStatus.INTERNAL_SERVER_ERROR
		);
	}

	@ExceptionHandler(WebExchangeBindException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(WebExchangeBindException ex) {

		String validationErrors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));

		log.error("Validation error: {}", validationErrors);

		return new ResponseEntity<>(
				new ErrorResponse(
						"Validation failed",
						"VALIDATION_ERROR",
						LocalDateTime.now(),
						validationErrors
				),
				HttpStatus.BAD_REQUEST
		);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
		log.error("Unexpected error: {}", ex.getMessage(), ex);

		return new ResponseEntity<>(
				new ErrorResponse(
						"An unexpected error occurred",
						"INTERNAL_SERVER_ERROR",
						LocalDateTime.now(),
						"Please contact support if the problem persists."
				),
				HttpStatus.INTERNAL_SERVER_ERROR
		);
	}
}