package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Message("customer.email-verified.event")
public class CustomerEmailVerifiedEvent extends CustomerEvent {
	private String email;

	@Builder
	public CustomerEmailVerifiedEvent(UUID customerId, String email,
									  Instant timestamp, int version) {
		super(customerId, version, timestamp);
		this.customerId = customerId;
		this.email = email;
		this.version = version;
	}
}
