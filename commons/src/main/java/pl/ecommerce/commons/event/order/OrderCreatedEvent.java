package pl.ecommerce.commons.event.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("order.created.event")
@NoArgsConstructor
public class OrderCreatedEvent extends OrderEvent {
	private List<OrderItem> items;

	@JsonCreator
	@Builder
	public OrderCreatedEvent(Instant timestamp, int version, UUID orderId, List<OrderItem> items) {
		super(orderId, version, timestamp);
		this.items = items;
	}

	@Builder
	@Getter
	public static class OrderItem {
		@JsonProperty("productId")
		private UUID productId;
		@JsonProperty("quantity")
		private int quantity;
	}
}
