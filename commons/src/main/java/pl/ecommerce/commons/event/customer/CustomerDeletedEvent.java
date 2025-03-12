package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("customer.deleted.event")
@NoArgsConstructor
public class CustomerDeletedEvent extends DomainEvent {
	private UUID customerId;
	private String email;
	private String firstName;
	private String lastName;

	@Builder
	public CustomerDeletedEvent(UUID correlationId, UUID customerId, String email, String firstName, String lastName) {
		super(correlationId);
		this.customerId = customerId;
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
	}
}
