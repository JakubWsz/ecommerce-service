package pl.ecommerce.customer.write.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomerAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleCustomerAlreadyExists(CustomerAlreadyExistsException ex) {
		return new ResponseEntity<>(new ErrorResponse(ex.getMessage(), "CUSTOMER_ALREADY_EXISTS",
				LocalDateTime.now(), "Try a different email or username.", ex.getTraceId()),
				HttpStatus.CONFLICT);
	}

	@ExceptionHandler(CustomerNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException ex) {
		return new ResponseEntity<>(new ErrorResponse(ex.getMessage(), "CUSTOMER_NOT_FOUND",
				LocalDateTime.now(), "Ensure the customer ID is correct.", ex.getTraceId()),
				HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(GdprConsentRequiredException.class)
	public ResponseEntity<ErrorResponse> handleGdprConsentRequired(GdprConsentRequiredException ex) {
		return new ResponseEntity<>(new ErrorResponse(ex.getMessage(), "GDPR_CONSENT_REQUIRED",
				LocalDateTime.now(), "User must provide consent before registration.", ex.getTraceId()),
				HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler(InternalAppException.class)
	public ResponseEntity<ErrorResponse> handleInternalAppException(InternalAppException ex) {
		return new ResponseEntity<>(new ErrorResponse(ex.getMessage(), "INTERNAL_ERROR",
				LocalDateTime.now(), "Unexpected error occurred.", ex.getTraceId()),
				HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
		return new ResponseEntity<>(new ErrorResponse("An unexpected error occurred", "UNKNOWN_ERROR",
				LocalDateTime.now(), ex.getMessage(), null),
				HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
