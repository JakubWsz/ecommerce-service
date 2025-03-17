package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Message("customer.email-changed.event")
public class CustomerEmailChangedEvent extends CustomerEvent {
	private String oldEmail;
	private String newEmail;

	@Builder
	public CustomerEmailChangedEvent(UUID customerId, String oldEmail,
									 String newEmail, Instant timestamp, int version) {
		super(customerId, version, timestamp);
		this.oldEmail = oldEmail;
		this.newEmail = newEmail;
	}
}