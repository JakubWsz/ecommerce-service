package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("vendor.status.changed.event")
@NoArgsConstructor
public class VendorStatusChangedEvent extends VendorEvent {
	private String oldStatus;
	private String newStatus;
	private String reason;

	@Builder
	public VendorStatusChangedEvent(UUID vendorId, String oldStatus,
									String newStatus, String reason, int version, Instant timestamp) {
		super(vendorId, version, timestamp);
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
		this.reason = reason;
	}
}