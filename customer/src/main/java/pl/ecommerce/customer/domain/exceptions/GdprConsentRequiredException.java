package pl.ecommerce.customer.domain.exceptions;

public class GdprConsentRequiredException extends CustomerException {
	public GdprConsentRequiredException(String message) {
		super(message);
	}
}
