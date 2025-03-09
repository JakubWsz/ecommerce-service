package pl.ecommerce.vendor.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(DocumentNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleDocumentNotFoundException(DocumentNotFoundException ex) {
		return buildResponseEntity("Document not found", "DOCUMENT_NOT_FOUND", HttpStatus.NOT_FOUND, ex);
	}

	@ExceptionHandler(CategoryAssignmentException.class)
	public ResponseEntity<ErrorResponse> handleCategoryAssignmentException(CategoryAssignmentException ex) {
		return buildResponseEntity("Category assignment failed", "CATEGORY_ASSIGNMENT_ERROR", HttpStatus.BAD_REQUEST, ex);
	}

	@ExceptionHandler(PaymentProcessingException.class)
	public ResponseEntity<ErrorResponse> handlePaymentProcessingException(PaymentProcessingException ex) {
		return buildResponseEntity("Payment processing error", "PAYMENT_PROCESSING_ERROR", HttpStatus.PAYMENT_REQUIRED, ex);
	}

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
		return buildResponseEntity("Validation failed", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST, ex);
	}

	@ExceptionHandler(VendorAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleVendorAlreadyExistsException(VendorAlreadyExistsException ex) {
		return buildResponseEntity("Vendor already exists", "VENDOR_ALREADY_EXISTS", HttpStatus.CONFLICT, ex);
	}

	@ExceptionHandler(VendorNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleVendorNotFoundException(VendorNotFoundException ex) {
		return buildResponseEntity("Vendor not found", "VENDOR_NOT_FOUND", HttpStatus.NOT_FOUND, ex);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
		return buildResponseEntity("An unexpected error occurred", "UNKNOWN_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, ex);
	}

	private ResponseEntity<ErrorResponse> buildResponseEntity(String message, String errorCode, HttpStatus status, Exception ex) {
		ErrorResponse errorResponse = new ErrorResponse(message, errorCode, LocalDateTime.now(), ex.getMessage());
		return new ResponseEntity<>(errorResponse, status);
	}
}
