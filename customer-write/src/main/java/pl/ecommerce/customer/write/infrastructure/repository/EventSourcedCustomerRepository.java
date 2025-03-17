package pl.ecommerce.customer.write.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.customer.write.domain.aggregate.*;
import pl.ecommerce.customer.write.infrastructure.eventstore.EventStore;
import pl.ecommerce.commons.kafka.EventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

import static java.util.Objects.nonNull;

@Repository
@RequiredArgsConstructor
@Slf4j
public class EventSourcedCustomerRepository implements CustomerRepository {

	private final EventStore eventStore;
	private final EventPublisher eventPublisher;
	private final JdbcTemplate jdbcTemplate;

	@Override
	@Transactional
	public Mono<CustomerAggregate> save(CustomerAggregate customer) {
		List<DomainEvent> uncommittedEvents = customer.getUncommittedEvents();

		if (uncommittedEvents.isEmpty()) {
			return Mono.just(customer);
		}

		return Mono.fromCallable(() -> persistEvents(customer, uncommittedEvents))
				.flatMap(savedCustomer -> publishEvents(uncommittedEvents)
						.then(Mono.fromRunnable(savedCustomer::clearUncommittedEvents))
						.thenReturn(savedCustomer));

	}

	@Override
	@Transactional(readOnly = true)
	public Mono<CustomerAggregate> findById(UUID customerId) {
		return Mono.fromCallable(() -> loadCustomerAggregate(customerId))
				.filter(Objects::nonNull);
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<Boolean> existsByEmail(String email) {
		return Mono.fromCallable(() -> existsByEmailInternal(email))
				.subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<CustomerAggregate> findByEmail(String email) {
		return Mono.fromCallable(() -> findCustomerIdByEmail(email))
				.subscribeOn(Schedulers.boundedElastic())
				.filter(Objects::nonNull)
				.flatMap(this::findById);
	}

	@Override
	@Transactional
	public Mono<Void> hardDelete(UUID customerId) {
		return Mono.fromRunnable(() -> {
			try {
				eventStore.markEventsAsDeleted(customerId);
				log.info("Hard deleted customer: {}", customerId);
			} catch (Exception e) {
				log.error("Error hard deleting customer {}: {}",
						customerId, e.getMessage(), e);
				throw e;
			}
		});
	}

	private CustomerAggregate persistEvents(CustomerAggregate customer, List<DomainEvent> events) {
		int expectedVersion = customer.getVersion() - events.size();
		eventStore.saveEvents(customer.getId(), events, expectedVersion);

		DomainEvent firstEvent = events.getFirst();
		String traceId = nonNull(firstEvent.getTracingContext())
				? firstEvent.getTracingContext().getTraceId()
				: "unknown";
		log.debug("Saved aggregate {} with {} events, traceId: {}",
				customer.getId(), events.size(), traceId);

		return customer;
	}

	private Mono<Void> publishEvents(List<DomainEvent> events) {
		return Flux.fromIterable(events)
				.doOnNext(event -> log.info("Publishing event: {} with type: {}", event.getClass().getSimpleName(), event.getEventType()))
				.flatMap(event -> eventPublisher.publish(event, event.getEventType(), createHeaders(event)))
				.doOnError(e -> log.error("Error publishing event", e))
				.then();
	}

	private Map<String, String> createHeaders(DomainEvent event) {
		String traceId = nonNull(event.getTracingContext())
				? event.getTracingContext().getTraceId()
				: "unknown";
		return Collections.singletonMap("trace-id", traceId);
	}

	private CustomerAggregate loadCustomerAggregate(UUID customerId) {
		try {
			List<DomainEvent> events = eventStore.getEventsForAggregate(customerId);
			if (events.isEmpty()) {
				log.debug("No events found for customer: {}", customerId);
				return null;
			}
			String traceId = extractTraceId(events);
			CustomerAggregate customer = new CustomerAggregate(events);
			log.debug("Reconstituted customer {} from {} events with traceId: {}",
					customerId, events.size(), traceId);
			return customer;
		} catch (Exception e) {
			log.error("Error finding customer by id {}: {}", customerId, e.getMessage(), e);
			throw e;
		}
	}

	private String extractTraceId(List<DomainEvent> events) {
		DomainEvent firstEvent = events.getFirst();
		return nonNull(firstEvent.getTracingContext())
				? firstEvent.getTracingContext().getTraceId()
				: "unknown";
	}

	private Boolean existsByEmailInternal(String email) {
		final String sql = "SELECT COUNT(*) FROM customer_email_view WHERE email = ?";
		try {
			Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
			return count != null && count > 0;
		} catch (DataAccessException e) {
			log.error("Error checking if customer exists by email {}: {}", email, e.getMessage(), e);
			throw e;
		}
	}

	private UUID findCustomerIdByEmail(String email) {
		final String sql = "SELECT customer_id FROM customer_email_view WHERE email = ? LIMIT 1";
		try {
			List<UUID> customerIds = jdbcTemplate.query(
					sql,
					(rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
					email
			);
			return customerIds.isEmpty() ? null : customerIds.get(0);
		} catch (DataAccessException e) {
			log.error("Error finding customer ID by email {}: {}", email, e.getMessage(), e);
			throw e;
		}
	}
}