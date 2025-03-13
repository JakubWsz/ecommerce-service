package pl.ecommerce.commons.event.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("order.confirmed.event")
@NoArgsConstructor
public class OrderConfirmedEvent extends OrderEvent {

	@JsonCreator
	@Builder
	public OrderConfirmedEvent(Instant timestamp, int version, UUID orderId) {
		super(orderId, version, timestamp);
	}
}