package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("vendor.updated.event")
@NoArgsConstructor
public class VendorUpdatedEvent extends VendorEvent {
	private Map<String, Object> changes;

	@Builder
	public VendorUpdatedEvent(UUID correlationId, UUID vendorId,
							  Map<String, Object> changes, int version, Instant timestamp) {
		super(vendorId, version, timestamp);
		this.changes = changes;
	}
}
