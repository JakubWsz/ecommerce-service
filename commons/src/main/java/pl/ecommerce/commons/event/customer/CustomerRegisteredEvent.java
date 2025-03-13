package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("customer.registered.event")
public class CustomerRegisteredEvent extends CustomerEvent {
	private String email;
	private String firstName;
	private String lastName;
	private String phoneNumber;

	@Builder
	public CustomerRegisteredEvent(UUID customerId, String email,
								   String firstName, String lastName, String phoneNumber,
								   Instant timestamp, int version) {
		super(customerId, version, timestamp);
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
		this.phoneNumber = phoneNumber;
	}

	public CustomerRegisteredEvent(UUID customerId, int version, Instant timestamp) {
		super(customerId, version, timestamp);
	}
}
