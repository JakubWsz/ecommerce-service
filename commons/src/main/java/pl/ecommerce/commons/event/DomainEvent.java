package pl.ecommerce.commons.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.ecommerce.commons.event.customer.CustomerDeletedEvent;
import pl.ecommerce.commons.event.customer.CustomerRegisteredEvent;
import pl.ecommerce.commons.event.customer.CustomerUpdatedEvent;
import pl.ecommerce.commons.event.product.ProductCreatedEvent;
import pl.ecommerce.commons.event.product.ProductUpdatedEvent;
import pl.ecommerce.commons.event.vendor.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes({
		@JsonSubTypes.Type(value = CustomerRegisteredEvent.class, name = "CustomerRegisteredEvent"),
		@JsonSubTypes.Type(value = CustomerDeletedEvent.class, name = "CustomerDeletedEvent"),
		@JsonSubTypes.Type(value = CustomerUpdatedEvent.class, name = "CustomerUpdatedEvent"),
		@JsonSubTypes.Type(value = ProductCreatedEvent.class, name = "ProductCreatedEvent"),
		@JsonSubTypes.Type(value = ProductUpdatedEvent.class, name = "ProductUpdatedEvent"),
		@JsonSubTypes.Type(value = VendorPaymentProcessedEvent.class, name = "VendorPaymentProcessedEvent"),
		@JsonSubTypes.Type(value = VendorCategoriesAssignedEvent.class, name = "VendorCategoriesAssignedEvent"),
		@JsonSubTypes.Type(value = VendorRegisteredEvent.class, name = "VendorRegisteredEvent"),
		@JsonSubTypes.Type(value = VendorStatusChangedEvent.class, name = "VendorStatusChangedEvent"),
		@JsonSubTypes.Type(value = VendorUpdatedEvent.class, name = "VendorUpdatedEvent"),
		@JsonSubTypes.Type(value = VendorVerificationCompletedEvent.class, name = "VendorVerificationCompletedEvent")
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