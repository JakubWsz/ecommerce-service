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
@Message("product.reservation.confirmed.event")
public class ProductReservationConfirmedEvent extends ProductEvent {
	private int quantity;
	private UUID reservationId;
	private UUID orderId;

	@Builder
	public ProductReservationConfirmedEvent(UUID productId, int quantity, UUID reservationId,
											UUID orderId, int version, Instant timestamp,
											@JsonProperty("vendorId") UUID vendorId) {
		super(productId, version, timestamp,vendorId);
		this.quantity = quantity;
		this.reservationId = reservationId;
		this.orderId = orderId;
	}
}