package pl.ecommerce.customer.write.infrastructure.repository;

import io.opentelemetry.context.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.infrastructure.eventstore.EventStore;
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
//  @Transactional // Na razie pozostaw zakomentowane dla testu contextCapture
	public Mono<CustomerAggregate> save(CustomerAggregate customer) {
		List<DomainEvent> uncommittedEvents = customer.getUncommittedEvents();

		if (uncommittedEvents.isEmpty()) {
			return Mono.just(customer);
		}

		return Mono.deferContextual(contextView -> {
					logContext(">>> Context at start of save.deferContextual", contextView);

					Mono<CustomerAggregate> operationChain = Mono.fromCallable(() -> {
								logContext(">>> Context before persistEvents (inside fromCallable)");
								return persistEvents(customer, uncommittedEvents);
							})
							.subscribeOn(Schedulers.boundedElastic())
							.flatMap(savedCustomer -> {
								logContext(">>> Context before publishEvents (inside flatMap)");
								return publishEvents(uncommittedEvents)
										.then(Mono.fromRunnable(savedCustomer::clearUncommittedEvents))
										.thenReturn(savedCustomer);
							});

					return operationChain.contextCapture();

				}
		);
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
		logContext(">>> Context at start of persistEvents (on BoundedElastic?)");

		int expectedVersion = customer.getVersion() - events.size();

		try {
			eventStore.saveEvents(customer.getId(), events, expectedVersion);
		} catch (Exception e) {
			log.error("Exception occurred during eventStore.saveEvents for aggregate {}: {}",
					customer.getId(), e.getMessage(), e);
			throw e;
		}

		return customer;
	}

	private Mono<Void> publishEvents(List<DomainEvent> events) {
		logContext(">>> Context at start of publishEvents method");

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
			List<DomainEvent> events = eventStore.getEventsForAggregate(customerId);
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