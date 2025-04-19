package pl.ecommerce.vendor.write.infrastructure.eventstore;

import pl.ecommerce.commons.event.AbstractDomainEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface EventStore {
	Mono<Void> saveEvents(UUID aggregateId, List<AbstractDomainEvent> events);
	Flux<AbstractDomainEvent> getEvents(UUID aggregateId);
	Mono<Boolean> existsByEmail(String email);

}