package pl.ecommerce.customer.infrastructure.exception;

public class GdprConsentRequiredException extends RuntimeException {
	private static final String ERROR_GDPR_CONSENT_REQUIRED = "GDPR consent is required";

	public GdprConsentRequiredException() {
		super(ERROR_GDPR_CONSENT_REQUIRED);
	}

}