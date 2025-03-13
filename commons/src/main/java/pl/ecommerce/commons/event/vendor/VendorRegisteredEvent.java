package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("vendor.registered.event")
@NoArgsConstructor
public class VendorRegisteredEvent extends VendorEvent {
	private String name;
	private String email;
	private String status;

	@Builder
	public VendorRegisteredEvent( UUID vendorId, String name, String email,
								  String status,  int version, Instant timestamp) {
		super(vendorId, version, timestamp);
		this.name = name;
		this.email = email;
		this.status = status;
	}
}

