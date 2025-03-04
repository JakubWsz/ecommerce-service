package pl.ecommerce.customer.infrastructure.exception;

public class GdprConsentRequiredException extends RuntimeException {
	public GdprConsentRequiredException(String message) {
		super(message);
	}

}