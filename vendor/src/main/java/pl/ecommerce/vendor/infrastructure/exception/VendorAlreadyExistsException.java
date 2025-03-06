package pl.ecommerce.vendor.infrastructure.exception;

public class VendorAlreadyExistsException extends RuntimeException {

	public VendorAlreadyExistsException(String message) {
		super(message);
	}

	public VendorAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}
}