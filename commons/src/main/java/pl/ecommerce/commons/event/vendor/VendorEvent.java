package pl.ecommerce.commons.event.vendor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.ecommerce.commons.event.AbstractDomainEvent;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
public abstract class VendorEvent extends AbstractDomainEvent {
	@Getter
	private UUID vendorId;
	protected int version;
	@Getter
	protected Instant eventTimestamp;

	protected VendorEvent(UUID vendorId, int version, Instant timestamp) {
		this.vendorId = vendorId;
		this.version = version;
		this.eventTimestamp = timestamp != null ? timestamp : Instant.now();
	}

	@Override
	public UUID getAggregateId() {
		return vendorId;
	}

	@Override
	public int getVersion() {
		return version;
	}
}
