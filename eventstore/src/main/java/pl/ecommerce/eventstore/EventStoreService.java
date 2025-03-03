package pl.ecommerce.eventstore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import pl.ecommerce.commons.event.DomainEvent;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventStoreService {
	private final ReactiveMongoTemplate mongoTemplate;

	public void saveEvent(DomainEvent event) {
		log.info("Saving event: {} to Event Store", event.getClass().getSimpleName());
		mongoTemplate.save(event, "DomainEvent").subscribe();
	}

	public Flux<DomainEvent> getEventsByCorrelationId(UUID correlationId) {
		return mongoTemplate.find(
				query(where("correlationId").is(correlationId)),
				DomainEvent.class,
				"DomainEvent"
		);
	}
	public Flux<DomainEvent> getEventsByCustomerId(UUID customerId) {
		return mongoTemplate.find(
				query(where("customerId").is(customerId)),
				DomainEvent.class,
				"DomainEvent"
		);
	}

	public Flux<DomainEvent> getEventsByEmail(String email) {
		return mongoTemplate.find(
				query(where("email").is(email)),
				DomainEvent.class,
				"DomainEvent"
		);
	}

	public Flux<DomainEvent> getEventsByDate(Instant date) {
		return mongoTemplate.find(
				query(where("timestamp").gte(date).lt(date.plus(1, ChronoUnit.DAYS))),
				DomainEvent.class,
				"DomainEvent"
		);
	}

	public Flux<DomainEvent> getEventsBetweenDates(Instant from, Instant to) {
		return mongoTemplate.find(
				query(where("timestamp").gte(from).lte(to)),
				DomainEvent.class,
				"DomainEvent"
		);
	}
}