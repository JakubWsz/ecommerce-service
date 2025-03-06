package pl.ecommerce.vendor.infrastructure.exception;

public class VendorNotFoundException extends RuntimeException {

	/**
	 * Create a vendor not found exception with a message
	 *
	 * @param message The error message
	 */
	public VendorNotFoundException(String message) {
		super(message);
	}

	/**
	 * Create a vendor not found exception with a message and cause
	 *
	 * @param message The error message
	 * @param cause The underlying cause
	 */
	public VendorNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}