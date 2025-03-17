package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Message("customer.deleted.event")
public class CustomerDeletedEvent extends CustomerEvent {
	private String email;
	private String firstName;
	private String lastName;

	@Builder
	public CustomerDeletedEvent(UUID customerId, String email, String firstName, String lastName, String reason,
								Instant timestamp, int version) {
		super(customerId, version, timestamp);
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;

	}
}
