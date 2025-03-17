package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Message("customer.phone-verified.event")
public class CustomerPhoneVerifiedEvent extends CustomerEvent {
	private UUID customerId;
	private String phoneNumber;
	private Instant timestamp;
	private int version;

	@Builder
	public CustomerPhoneVerifiedEvent(UUID customerId, String phoneNumber,
									  Instant timestamp, int version) {
		super(customerId, version, timestamp);
		this.customerId = customerId;
		this.phoneNumber = phoneNumber;
		this.timestamp = timestamp;
		this.version = version;
	}

	@Override
	public UUID getAggregateId() {
		return customerId;
	}
}