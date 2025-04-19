package pl.ecommerce.customer.write.infrastructure.repository;

import io.opentelemetry.context.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.event.AbstractDomainEvent;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.infrastructure.eventstore.EventStore;
import pl.ecommerce.customer.write.infrastructure.exception.ConcurrencyException;
import pl.ecommerce.customer.write.infrastructure.exception.CustomerNotFoundException;
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
	public Mono<CustomerAggregate> save(CustomerAggregate customer) {
		List<AbstractDomainEvent> uncommittedEvents = customer.getUncommittedEvents();

		if (uncommittedEvents.isEmpty()) {
			return Mono.just(customer);
		}

		return Mono.deferContextual(contextView -> {
			logContext(">>> Context at start of save.deferContextual", contextView);

			return Mono.fromCallable(() -> {
						if (loadCustomerAggregate(customer.getId()) == null) {
							return customer;
						}

						CustomerAggregate freshAggregate = new CustomerAggregate(
								eventStore.getEventsForAggregate(customer.getId())
						);

						for (AbstractDomainEvent event : new ArrayList<>(uncommittedEvents)) {
							freshAggregate.getHelper().applyChange(event);
						}

						return freshAggregate;
					})
					.subscribeOn(Schedulers.boundedElastic())
					.flatMap(aggregateToSave -> {
						logContext(">>> Context before persistEvents (inside fromCallable)");

						return Mono.fromCallable(() -> {
									int expectedVersion = aggregateToSave.getVersion() - uncommittedEvents.size();
									eventStore.saveEvents(aggregateToSave.getId(), uncommittedEvents, expectedVersion);
									return aggregateToSave;
								})
								.subscribeOn(Schedulers.boundedElastic())
								.flatMap(savedAggregate -> {
									logContext(">>> Context before publishEvents (inside flatMap)");
									return publishEvents(uncommittedEvents)
											.then(Mono.fromRunnable(savedAggregate::clearUncommittedEvents))
											.thenReturn(savedAggregate);
								});
					})
					.retry(3)
					.onErrorResume(ConcurrencyException.class, ex -> {
						log.warn("Concurrency error occurred during save, retrying with fresh aggregate: {}", ex.getMessage());
						return Mono.error(ex);
					})
					.contextCapture();
		});
	}

	@Override
	@Transactional(readOnly = true)
	public Mono<CustomerAggregate> findById(UUID customerId) {
		return Mono.fromCallable(() -> loadCustomerAggregate(customerId))
				.filter(Objects::nonNull)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)));
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

	private CustomerAggregate persistEvents(CustomerAggregate customer, List<AbstractDomainEvent> events) {
		logContext(">>> Context at start of persistEvents (on BoundedElastic?)");

		int expectedVersion = customer.getVersion() - events.size();

		try {
			eventStore.saveEvents(customer.getId(), events, expectedVersion);

			customer.setVersion(customer.getVersion() + events.size());
		} catch (Exception e) {
			log.error("Exception occurred during eventStore.saveEvents for aggregate {}: {}",
					customer.getId(), e.getMessage(), e);
			throw e;
		}

		return customer;
	}

	private Mono<Void> publishEvents(List<AbstractDomainEvent> events) {
		logContext(">>> Context at start of publishEvents method");

		if (events.isEmpty()) {
			return Mono.empty();
		}

		return Flux.fromIterable(events)
				.flatMap(event -> {
					logContext(">>> Context before eventPublisher.publish for event: " + event.getEventType());
					return eventPublisher.publish(event);
				})
				.doOnError(e -> log.error("Błąd podczas publikowania strumienia zdarzeń: {}", e.getMessage(), e))
				.then();
	}


	private void logContext(String message) {
		try {
			io.opentelemetry.api.trace.Span currentSpan = io.opentelemetry.api.trace.Span.fromContext(Context.current());
			if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
				log.info("{} - Active OTel Span: traceId={}, spanId={}",
						message,
						currentSpan.getSpanContext().getTraceId(),
						currentSpan.getSpanContext().getSpanId());
			} else {
				log.warn("{} - No valid OTel span found!", message);
			}
		} catch (Exception e) {
			log.error("{} - Error checking OTel span", message, e);
		}
	}

	private void logContext(String message, reactor.util.context.ContextView contextView) {
		log.info("{} - Reactor ContextView present: {}", message, !contextView.isEmpty());
		logContext(message);
	}

	private CustomerAggregate loadCustomerAggregate(UUID customerId) {
		try {
			List<AbstractDomainEvent> events = eventStore.getEventsForAggregate(customerId);
			if (events.isEmpty()) {
				log.debug("No events found for customer: {}", customerId);
				return null;
			}

			return new CustomerAggregate(events);
		} catch (Exception e) {
			log.error("Error finding customer by id {}: {}", customerId, e.getMessage(), e);
			throw e;
		}
	}

	private Boolean existsByEmailInternal(String email) {
		final String sql = "SELECT COUNT(*) FROM customer_email_view WHERE email = ?";
		try {
			Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
			return nonNull(count) && count > 0;
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
			return customerIds.isEmpty() ? null : customerIds.getFirst();
		} catch (DataAccessException e) {
			log.error("Error finding customer ID by email {}: {}", email, e.getMessage(), e);
			throw e;
		}
	}
}