package pl.ecommerce.commons.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.ecommerce.commons.event.customer.CustomerDeletedEvent;
import pl.ecommerce.commons.event.customer.CustomerRegisteredEvent;
import pl.ecommerce.commons.event.customer.CustomerUpdatedEvent;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes({
		@JsonSubTypes.Type(value = CustomerRegisteredEvent.class, name = "CustomerRegisteredEvent"),
		@JsonSubTypes.Type(value = CustomerDeletedEvent.class, name = "CustomerDeletedEvent"),
		@JsonSubTypes.Type(value = CustomerUpdatedEvent.class, name = "CustomerUpdatedEvent")
})
public abstract class DomainEvent {
	private final UUID eventId = UUID.randomUUID();
	private final Instant timestamp = Instant.now();
	private UUID correlationId;

	@JsonCreator
	protected DomainEvent(@JsonProperty("correlationId") UUID correlationId) {
		this.correlationId = correlationId;
	}
}