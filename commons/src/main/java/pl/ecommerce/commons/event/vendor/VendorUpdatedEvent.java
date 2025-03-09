package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.updated.event")
public class VendorUpdatedEvent extends DomainEvent {
	private UUID vendorId;
	private Map<String, Object> changes;


	@Builder
	public VendorUpdatedEvent(UUID correlationId, UUID vendorId, Map<String, Object> changes) {
		super(correlationId);
		this.vendorId = vendorId;
		this.changes = changes;
	}
}
