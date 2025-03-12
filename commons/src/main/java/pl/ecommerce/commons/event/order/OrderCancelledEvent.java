package pl.ecommerce.commons.event.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("order.cancelled.event")
@NoArgsConstructor
public class OrderCancelledEvent extends DomainEvent {
	private UUID orderId;
	private String reason;

	@Builder
	public OrderCancelledEvent(@JsonProperty("correlationId") UUID correlationId,
							   @JsonProperty("orderId") UUID orderId,
							   @JsonProperty("reason") String reason) {
		super(correlationId);
		this.orderId = orderId;
		this.reason = reason;
	}
}
