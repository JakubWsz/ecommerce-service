package pl.ecommerce.vendor.infrastructure.exception;

public class CategoryAssignmentException extends RuntimeException {

	public CategoryAssignmentException(String message) {
		super(message);
	}

	public CategoryAssignmentException(String message, Throwable cause) {
		super(message, cause);
	}
}
