package pl.ecommerce.commons.customer.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AuthMethod {
	private AuthMethodType type;
	private String identifier;
	private boolean isVerified;
	private Instant lastUsed;

	public enum AuthMethodType {
		PASSWORD,
		SOCIAL_GOOGLE,
		SOCIAL_FACEBOOK,
		OAUTH2,
		TWO_FACTOR
	}
}

