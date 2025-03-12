package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("vendor.status.changed.event")
@NoArgsConstructor
public class VendorStatusChangedEvent extends DomainEvent {
	private UUID vendorId;
	private String oldStatus;
	private String newStatus;
	private String reason;

	@Builder
	public VendorStatusChangedEvent(UUID correlationId, UUID vendorId,
									String oldStatus, String newStatus, String reason) {
		super(correlationId);
		this.vendorId = vendorId;
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
		this.reason = reason;
	}
}