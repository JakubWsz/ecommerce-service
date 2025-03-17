package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.category-assigned.event")
public class VendorCategoryAssignedEvent extends VendorEvent {
	private UUID categoryId;
	private String categoryName;

	@Builder
	public VendorCategoryAssignedEvent(UUID vendorId, UUID categoryId,
									   String categoryName, Instant timestamp,
									   int version) {
		super(vendorId, version, timestamp);
		this.categoryId = categoryId;
		this.categoryName = categoryName;
	}
}