package pl.ecommerce.customer.write.infrastructure.eventstore;

import pl.ecommerce.commons.event.AbstractDomainEvent;

import java.util.List;
import java.util.UUID;

public interface EventStore {

	void saveEvents(UUID aggregateId, List<AbstractDomainEvent> events, int expectedVersion);

	List<AbstractDomainEvent> getEventsForAggregate(UUID aggregateId);

	void markEventsAsDeleted(UUID aggregateId);
}