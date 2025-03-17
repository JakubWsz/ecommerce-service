package pl.ecommerce.customer.write.infrastructure.eventstore;

import pl.ecommerce.commons.event.DomainEvent;

import java.util.List;
import java.util.UUID;

public interface EventStore {

	void saveEvents(UUID aggregateId, List<DomainEvent> events, int expectedVersion);

	List<DomainEvent> getEventsForAggregate(UUID aggregateId);

	void markEventsAsDeleted(UUID aggregateId);
}