package pl.ecommerce.commons.event.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("product.reserved.event")
public class ProductReservedEvent extends ProductEvent {
	private int quantity;
	private UUID reservationId;

	@Builder
	public ProductReservedEvent(UUID productId, int quantity, UUID reservationId,
								int version, Instant timestamp, @JsonProperty("vendorId") UUID vendorId) {
		super(productId, version, timestamp, vendorId);
		this.quantity = quantity;
		this.reservationId = reservationId;
	}
}
