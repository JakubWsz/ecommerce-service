package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Message("customer.deactivated.event")
public class CustomerDeactivatedEvent extends CustomerEvent {
	private String reason;

	@Builder
	public CustomerDeactivatedEvent(UUID customerId, String reason,
									Instant timestamp, int version) {
		super(customerId, version, timestamp);
		this.reason = reason;
	}
}