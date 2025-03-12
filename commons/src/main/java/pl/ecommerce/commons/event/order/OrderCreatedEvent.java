package pl.ecommerce.commons.event.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("order.created.event")
@NoArgsConstructor
public class OrderCreatedEvent extends DomainEvent {
	private UUID orderId;
	private List<OrderItem> items;

	@JsonCreator
	@Builder
	public OrderCreatedEvent(
			@JsonProperty("correlationId") UUID correlationId,
			@JsonProperty("orderId") UUID orderId,
			@JsonProperty("items") List<OrderItem> items) {
		super(correlationId);
		this.orderId = orderId;
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
