package pl.ecommerce.vendor.write.infrastructure.exception;

public class InvalidVendorDataException extends RuntimeException {

	public InvalidVendorDataException(String message) {
		super(message);
	}
}