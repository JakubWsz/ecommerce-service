package pl.ecommerce.customer.write.infrastructure.exception;

public class GdprConsentRequiredException extends CustomerException {
	public GdprConsentRequiredException(String message) {
		super(message);
	}

	public GdprConsentRequiredException(String message, String traceId) {
		super(message);
	}
}
