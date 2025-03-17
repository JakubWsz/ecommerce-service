package pl.ecommerce.commons.event.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("order.cancelled.event")
@NoArgsConstructor
public class OrderCancelledEvent extends OrderEvent {
	private String reason;

	@JsonCreator
	@Builder
	public OrderCancelledEvent(Instant timestamp, int version, UUID orderId, String reason) {
		super(orderId, version, timestamp);
		this.reason = reason;
	}
}
