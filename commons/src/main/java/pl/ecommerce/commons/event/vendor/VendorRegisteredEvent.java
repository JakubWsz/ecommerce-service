package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.util.UUID;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.registered.event")
public class VendorRegisteredEvent extends DomainEvent {
	private UUID vendorId;
	private String name;
	private String email;
	private String status;

	@Builder
	public VendorRegisteredEvent(UUID correlationId, UUID vendorId, String name, String email, String status) {
		super(correlationId);
		this.vendorId = vendorId;
		this.name = name;
		this.email = email;
		this.status = status;
	}
}

