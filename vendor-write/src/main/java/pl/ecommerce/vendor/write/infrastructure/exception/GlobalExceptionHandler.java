package pl.ecommerce.vendor.write.infrastructure.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(VendorAlreadyExistsException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ResponseEntity<ErrorResponse> handleVendorAlreadyExistsException(VendorAlreadyExistsException ex) {
		log.error("Vendor already exists: {}, traceId: {}", ex.getMessage(), ex.getTraceId());
		ErrorResponse error = new ErrorResponse(
				HttpStatus.CONFLICT.value(),
				"VENDOR_ALREADY_EXISTS",
				ex.getMessage(),
				Instant.now(),
				ex.getTraceId()
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(VendorNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ResponseEntity<ErrorResponse> handleVendorNotFoundException(VendorNotFoundException ex) {
		log.error("Vendor not found: {}", ex.getMessage());
		ErrorResponse error = new ErrorResponse(
				HttpStatus.NOT_FOUND.value(),
				"VENDOR_NOT_FOUND",
				ex.getMessage(),
				Instant.now(),
				null
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(CategoryNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ResponseEntity<ErrorResponse> handleCategoryNotFoundException(CategoryNotFoundException ex) {
		log.error("Category not found: {}", ex.getMessage());
		ErrorResponse error = new ErrorResponse(
				HttpStatus.NOT_FOUND.value(),
				"CATEGORY_NOT_FOUND",
				ex.getMessage(),
				Instant.now(),
				null
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(CategoryAlreadyAssignedException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ResponseEntity<ErrorResponse> handleCategoryAlreadyAssignedException(CategoryAlreadyAssignedException ex) {
		log.error("Category already assigned: {}", ex.getMessage());
		ErrorResponse error = new ErrorResponse(
				HttpStatus.CONFLICT.value(),
				"CATEGORY_ALREADY_ASSIGNED",
				ex.getMessage(),
				Instant.now(),
				null
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(VendorNotActiveException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<ErrorResponse> handleVendorNotActiveException(VendorNotActiveException ex) {
		log.error("Vendor not active: {}", ex.getMessage());
		ErrorResponse error = new ErrorResponse(
				HttpStatus.BAD_REQUEST.value(),
				"VENDOR_NOT_ACTIVE",
				ex.getMessage(),
				Instant.now(),
				null
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(InvalidVendorDataException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<ErrorResponse> handleInvalidVendorDataException(InvalidVendorDataException ex) {
		log.error("Invalid vendor data: {}", ex.getMessage());
		ErrorResponse error = new ErrorResponse(
				HttpStatus.BAD_REQUEST.value(),
				"INVALID_VENDOR_DATA",
				ex.getMessage(),
				Instant.now(),
				null
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(EventStoreException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ResponseEntity<ErrorResponse> handleEventStoreException(EventStoreException ex) {
		log.error("Event store error: {}", ex.getMessage(), ex);
		ErrorResponse error = new ErrorResponse(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"EVENT_STORE_ERROR",
				"An error occurred while processing the event store operation",
				Instant.now(),
				null
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	@ExceptionHandler(WebExchangeBindException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(WebExchangeBindException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		ValidationErrorResponse response = new ValidationErrorResponse(
				HttpStatus.BAD_REQUEST.value(),
				"VALIDATION_ERROR",
				"Validation failed",
				Instant.now(),
				errors
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
		log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
		ErrorResponse error = new ErrorResponse(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"INTERNAL_SERVER_ERROR",
				"An unexpected error occurred",
				Instant.now(),
				null
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	@Getter
	public static class ErrorResponse {
		private final int status;
		private final String code;
		private final String message;
		private final Instant timestamp;
		private final String traceId;

		public ErrorResponse(int status, String code, String message, Instant timestamp, String traceId) {
			this.status = status;
			this.code = code;
			this.message = message;
			this.timestamp = timestamp;
			this.traceId = traceId;
		}

	}

	@Getter
	public static class ValidationErrorResponse extends ErrorResponse {
		private final Map<String, String> errors;

		public ValidationErrorResponse(int status, String code, String message, Instant timestamp, Map<String, String> errors) {
			super(status, code, message, timestamp, null);
			this.errors = errors;
		}

	}
}