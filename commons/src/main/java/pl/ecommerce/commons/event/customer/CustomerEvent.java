package pl.ecommerce.commons.event.customer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.ecommerce.commons.event.AbstractDomainEvent;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
public abstract class CustomerEvent extends AbstractDomainEvent {
	@Getter
	protected UUID customerId;
	protected int version;
	@Getter
	protected Instant eventTimestamp;

	protected CustomerEvent(UUID customerId, int version, Instant timestamp) {
		this.customerId = customerId;
		this.version = version;
		this.eventTimestamp = timestamp != null ? timestamp : Instant.now();
	}

	@Override
	public UUID getAggregateId() {
		return customerId;
	}

	@Override
	public int getVersion() {
		return version;
	}
}