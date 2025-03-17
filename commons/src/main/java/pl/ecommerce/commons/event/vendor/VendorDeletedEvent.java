package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.deleted.event")
public class VendorDeletedEvent extends VendorEvent {
	private String reason;

	@Builder
	public VendorDeletedEvent(UUID vendorId, String reason, Instant timestamp, int version) {
		super(vendorId, version, timestamp);
		this.reason = reason;
	}
}