package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.registered.event")
public class VendorRegisteredEvent extends DomainEvent {
	private UUID vendorId;
	private String name;
	private String email;
	private Set<Map<UUID,String>>  productCategories;
	private String status;

	@Builder
	public VendorRegisteredEvent(UUID correlationId, UUID vendorId, String name, String email,
								 Set<Map<UUID,String>> productCategories, String status) {
		super(correlationId);
		this.vendorId = vendorId;
		this.name = name;
		this.email = email;
		this.productCategories = productCategories;
		this.status = status;
	}
}

