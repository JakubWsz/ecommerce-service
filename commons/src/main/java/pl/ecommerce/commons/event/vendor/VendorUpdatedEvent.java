package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.updated.event")
public class VendorUpdatedEvent extends VendorEvent {
	private Map<String, Object> changes;

	@Builder
	public VendorUpdatedEvent(UUID vendorId, Map<String, Object> changes, Instant timestamp, int version) {
		super(vendorId, version, timestamp);
		this.changes = changes;
	}
}