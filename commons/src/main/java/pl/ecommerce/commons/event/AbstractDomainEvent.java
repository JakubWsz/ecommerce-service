package pl.ecommerce.commons.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micrometer.observation.Observation;
import lombok.Getter;
import lombok.Setter;
import pl.ecommerce.commons.event.customer.*;
import pl.ecommerce.commons.event.product.ProductCreatedEvent;
import pl.ecommerce.commons.event.product.ProductUpdatedEvent;
import pl.ecommerce.commons.event.vendor.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes({
		@JsonSubTypes.Type(value = CustomerRegisteredEvent.class, name = "CustomerRegisteredEvent"),
		@JsonSubTypes.Type(value = CustomerDeletedEvent.class, name = "CustomerDeletedEvent"),
		@JsonSubTypes.Type(value = CustomerUpdatedEvent.class, name = "CustomerUpdatedEvent"),
		@JsonSubTypes.Type(value = CustomerAddressAddedEvent.class, name = "CustomerAddressAddedEvent"),
		@JsonSubTypes.Type(value = CustomerAddressRemovedEvent.class, name = "CustomerAddressRemovedEvent"),
		@JsonSubTypes.Type(value = CustomerAddressUpdatedEvent.class, name = "CustomerAddressUpdatedEvent"),
		@JsonSubTypes.Type(value = CustomerDeactivatedEvent.class, name = "CustomerDeactivatedEvent"),
		@JsonSubTypes.Type(value = CustomerEmailChangedEvent.class, name = "CustomerEmailChangedEvent"),
		@JsonSubTypes.Type(value = CustomerEmailVerifiedEvent.class, name = "CustomerEmailVerifiedEvent"),
		@JsonSubTypes.Type(value = CustomerPhoneVerifiedEvent.class, name = "CustomerPhoneVerifiedEvent"),
		@JsonSubTypes.Type(value = CustomerPreferencesUpdatedEvent.class, name = "CustomerPreferencesUpdatedEvent"),
		@JsonSubTypes.Type(value = CustomerReactivatedEvent.class, name = "CustomerReactivatedEvent"),
		@JsonSubTypes.Type(value = ProductCreatedEvent.class, name = "ProductCreatedEvent"),
		@JsonSubTypes.Type(value = ProductUpdatedEvent.class, name = "ProductUpdatedEvent"),
		@JsonSubTypes.Type(value = VendorPaymentProcessedEvent.class, name = "VendorPaymentProcessedEvent"),
//		@JsonSubTypes.Type(value = VendorCategoriesAssignedEvent.class, name = "VendorCategoriesAssignedEvent"),
		@JsonSubTypes.Type(value = VendorRegisteredEvent.class, name = "VendorRegisteredEvent"),
		@JsonSubTypes.Type(value = VendorStatusChangedEvent.class, name = "VendorStatusChangedEvent"),
		@JsonSubTypes.Type(value = VendorUpdatedEvent.class, name = "VendorUpdatedEvent"),
		@JsonSubTypes.Type(value = VendorVerificationCompletedEvent.class, name = "VendorVerificationCompletedEvent")
})
public abstract class AbstractDomainEvent implements DomainEvent {
	private final UUID eventId;
	private final Instant timestamp;

	@Setter
	@JsonIgnore
	private Observation.ContextView tracingContext;

	protected AbstractDomainEvent() {
		this.eventId = UUID.randomUUID();
		this.timestamp = Instant.now();
	}

	@Override
	public String getEventType() {
		return this.getClass().getSimpleName();
	}

	@Setter
	@Getter
	@JsonIgnore
	private String traceId;

	@Setter
	@Getter
	@JsonIgnore
	private String spanId;
}