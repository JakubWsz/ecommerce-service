package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Message("customer.address-removed.event")
public class CustomerAddressRemovedEvent extends CustomerEvent {
	private UUID addressId;

	@Builder
	public CustomerAddressRemovedEvent(UUID customerId, UUID addressId,
									   Instant timestamp, int version) {
		super(customerId, version, timestamp);
		this.addressId = addressId;
	}
}