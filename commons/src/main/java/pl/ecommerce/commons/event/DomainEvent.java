package pl.ecommerce.commons.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

	UUID getEventId();

	UUID getAggregateId();

	String getAggregateType();

	int getVersion();

	Instant getTimestamp();

	String getEventType();
}