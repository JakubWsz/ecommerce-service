package pl.ecommerce.commons.event.product;

import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.ecommerce.commons.event.AbstractDomainEvent;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
public abstract class ProductEvent extends AbstractDomainEvent {
	@Getter
	protected UUID productId;
	protected int version;
	@Getter
	protected Instant eventTimestamp;
	protected UUID vendorId;

	protected ProductEvent(UUID productId, int version, Instant timestamp, UUID vendorId) {
		this.productId = productId;
		this.version = version;
		this.eventTimestamp = timestamp != null ? timestamp : Instant.now();
		this.vendorId = vendorId;
	}

	@Override
	public UUID getAggregateId() {
		return productId;
	}

	@Override
	public String getAggregateType() {
		return "Product";
	}

	@Override
	public int getVersion() {
		return version;
	}
}
