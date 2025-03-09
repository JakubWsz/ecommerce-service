package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("customer.updated.event")
public class CustomerUpdatedEvent extends DomainEvent {
	private UUID customerId;
	private Map<String, Object> changes;

	@Builder
	public CustomerUpdatedEvent(UUID correlationId, UUID customerId, Map<String, Object> changes) {
		super(correlationId);
		this.customerId = customerId;
		this.changes = changes;
	}
}
