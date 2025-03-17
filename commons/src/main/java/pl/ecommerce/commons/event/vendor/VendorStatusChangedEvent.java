package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;
import pl.ecommerce.commons.model.vendor.VendorStatus;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.status-changed.event")
public class VendorStatusChangedEvent extends VendorEvent {
	private VendorStatus oldStatus;
	private VendorStatus newStatus;
	private String reason;

	@Builder
	public VendorStatusChangedEvent(UUID vendorId, VendorStatus oldStatus, VendorStatus newStatus, String reason,
									Instant timestamp, int version) {
		super(vendorId, version, timestamp);
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
		this.reason = reason;
	}
}