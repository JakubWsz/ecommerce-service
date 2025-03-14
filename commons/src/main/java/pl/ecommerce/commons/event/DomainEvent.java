package pl.ecommerce.commons.event;

import pl.ecommerce.commons.tracing.TracingContext;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

	UUID getEventId();

	UUID getAggregateId();

	String getAggregateType();

	int getVersion();

	Instant getTimestamp();

	String getEventType();

	TracingContext getTracingContext();

	void setTracingContext(TracingContext tracingContext);
}