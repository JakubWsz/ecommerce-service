package pl.ecommerce.customer.domain.valueobjects;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class AuthMethod {
	AuthMethodType type;
	String identifier;
	boolean isVerified;
	Instant lastUsed;

	public enum AuthMethodType {
		PASSWORD,
		SOCIAL_GOOGLE,
		SOCIAL_FACEBOOK,
		OAUTH2,
		TWO_FACTOR
	}
}
