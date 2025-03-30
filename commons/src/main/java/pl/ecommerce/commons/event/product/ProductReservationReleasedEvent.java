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
@Message("product.reservation.released.event")
public class ProductReservationReleasedEvent extends ProductEvent {
	private UUID reservationId;
	private String reason;

	@Builder
	public ProductReservationReleasedEvent(UUID productId, UUID reservationId, String reason,
										   int version, Instant timestamp,
										   @JsonProperty("vendorId") UUID vendorId) {
		super(productId, version, timestamp,vendorId);
		this.reservationId = reservationId;
		this.reason = reason;
	}
}