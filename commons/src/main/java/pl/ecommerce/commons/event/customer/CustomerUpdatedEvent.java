package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("customer.updated.event")
public class CustomerUpdatedEvent extends CustomerEvent {
	private Map<String, Object> changes;

	@Builder
	public CustomerUpdatedEvent(UUID customerId, Map<String, Object> changes,
								Instant timestamp, int version) {
		super(customerId, version, timestamp);
		this.changes = changes;
	}
}
