package pl.ecommerce.commons.event.order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.ecommerce.commons.event.AbstractDomainEvent;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
public abstract class OrderEvent extends AbstractDomainEvent {
	@Getter
	protected UUID orderId;
	protected int version;
	@Getter
	protected Instant eventTimestamp;

	protected OrderEvent(UUID orderId, int version, Instant timestamp) {
		this.orderId = orderId;
		this.version = version;
		this.eventTimestamp = timestamp != null ? timestamp : Instant.now();
	}

	@Override
	public UUID getAggregateId() {
		return orderId;
	}

	@Override
	public String getAggregateType() {
		return "Order";
	}

	@Override
	public int getVersion() {
		return version;
	}
}
