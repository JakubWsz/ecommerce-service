package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("vendor.categories.assigned.event")
@NoArgsConstructor
public class VendorCategoriesAssignedEvent extends VendorEvent {
	private List<CategoryAssignment> categories;

	@Builder
	public VendorCategoriesAssignedEvent(UUID vendorId, List<CategoryAssignment> categories, int version,
										 Instant timestamp) {
		super(vendorId,version,timestamp);
		this.categories = categories;
	}
}