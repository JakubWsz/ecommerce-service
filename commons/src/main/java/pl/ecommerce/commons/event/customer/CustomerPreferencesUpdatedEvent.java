package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.model.customer.CustomerPreferences;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Message("customer.preferences-updated.event")
public class CustomerPreferencesUpdatedEvent extends CustomerEvent {
	private CustomerPreferences preferences;

	@Builder
	public CustomerPreferencesUpdatedEvent(UUID customerId, CustomerPreferences preferences,
										   Instant timestamp, int version) {
		super(customerId, version, timestamp);
		this.preferences = preferences;
	}
}
