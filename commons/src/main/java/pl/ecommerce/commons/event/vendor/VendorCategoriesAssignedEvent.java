package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.dto.CategoryAssignmentDto;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.categories.assigned.event")
public class VendorCategoriesAssignedEvent extends DomainEvent {
	private UUID vendorId;
	private List<CategoryAssignmentDto> categories;

	@Builder
	public VendorCategoriesAssignedEvent(UUID correlationId, UUID vendorId, List<CategoryAssignmentDto> categories) {
		super(correlationId);
		this.vendorId = vendorId;
		this.categories = categories;
	}
}