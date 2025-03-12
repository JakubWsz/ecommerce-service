package pl.ecommerce.commons.event.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("order.confirmed.event")
@NoArgsConstructor
public class OrderConfirmedEvent extends DomainEvent {
	private UUID orderId;

	@Builder
	public OrderConfirmedEvent(@JsonProperty("correlationId") UUID correlationId,
							   @JsonProperty("orderId") UUID orderId) {
		super(correlationId);
		this.orderId = orderId;
	}
}