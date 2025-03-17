package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.category-removed.event")
public class VendorCategoryRemovedEvent extends VendorEvent {
	private UUID categoryId;

	@Builder
	public VendorCategoryRemovedEvent(UUID vendorId, UUID categoryId,
									  Instant timestamp, int version) {
		super(vendorId, version, timestamp);
		this.categoryId = categoryId;
	}
}