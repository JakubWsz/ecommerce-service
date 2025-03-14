package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Message("customer.reactivated.event")
public class CustomerReactivatedEvent extends CustomerEvent {

	@Builder
	public CustomerReactivatedEvent(UUID customerId,String note,
									Instant timestamp, int version) {
		super(customerId, version, timestamp);
	}
}
