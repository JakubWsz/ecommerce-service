package pl.ecommerce.vendor.write.infrastructure.eventstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.vendor.write.infrastructure.exception.EventStoreException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JdbcEventStore implements EventStore {

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;
	private final EventPublisher publisher;

	private static final String INSERT_EVENT_SQL =
			"INSERT INTO event_store (aggregate_id, aggregate_type, event_type, event_data, version, timestamp) " +
					"VALUES (?, ?, ?, ?, ?, ?)";

	private static final String SELECT_EVENTS_SQL =
			"SELECT * FROM event_store WHERE aggregate_id = ? ORDER BY version ASC";

	private static final String SELECT_LAST_VERSION_SQL =
			"SELECT MAX(version) FROM event_store WHERE aggregate_id = ?";

	@Override
	@Transactional
	public Mono<Void> saveEvents(UUID aggregateId, List<DomainEvent> events) {
		return Mono.fromCallable(() -> {
					try {
						Integer currentVersion = getCurrentVersion(aggregateId);
						for (DomainEvent event : events) {
							currentVersion = saveSingleVendorEvent(aggregateId, currentVersion, event);
						}
						return events;
					} catch (Exception e) {
						log.error("Error saving vendor events for aggregate {}: {}", aggregateId, e.getMessage(), e);
						throw new EventStoreException("Error saving vendor events: " + e.getMessage(), e);
					}
				})
				.flatMap(savedEvents -> Flux.fromIterable(savedEvents)
						.flatMap(this::publishEventToKafka)
						.then())
				.subscribeOn(Schedulers.boundedElastic());
	}


	@Override
	public Flux<DomainEvent> getEvents(UUID aggregateId) {
		return Mono.fromCallable(() -> {
					try {
						return jdbcTemplate.query(SELECT_EVENTS_SQL, new EventRowMapper(), aggregateId);
					} catch (DataAccessException e) {
						log.error("Error retrieving events for aggregate {}: {}", aggregateId, e.getMessage(), e);
						throw new EventStoreException("Error retrieving events: " + e.getMessage(), e);
					}
				})
				.flatMapIterable(events -> events)
				.subscribeOn(Schedulers.boundedElastic());
	}

	private int getCurrentVersion(UUID aggregateId) {
		Integer version = jdbcTemplate.queryForObject(
				"SELECT MAX(version) FROM event_store WHERE aggregate_id = ? AND deleted = false",
				Integer.class,
				aggregateId);
		return version != null ? version : 0;
	}

	@Override
	public Mono<Boolean> existsByEmail(String email) {
		String sql = "SELECT COUNT(*) FROM event_store WHERE LOWER(event_data->>'email') = LOWER(?);";
		return Mono.fromCallable(() -> {
					Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
					return count != null && count > 0;
				})
				.subscribeOn(Schedulers.boundedElastic())
				.doOnNext(exists -> log.debug("Vendor with email {} exists in DB: {}", email, exists));
	}


	private class EventRowMapper implements RowMapper<DomainEvent> {
		@Override
		public DomainEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
			try {
				String eventType = rs.getString("event_type");
				String eventData = rs.getString("event_data");

				Class<? extends DomainEvent> eventClass = getEventClass(eventType);
				return objectMapper.readValue(eventData, eventClass);
			} catch (IOException | ClassNotFoundException e) {
				throw new SQLException("Failed to deserialize event", e);
			}
		}

		@SuppressWarnings("unchecked")
		private Class<? extends DomainEvent> getEventClass(String eventType) throws ClassNotFoundException {
			String eventPackage = "pl.ecommerce.commons.event.vendor.";
			return (Class<? extends DomainEvent>) Class.forName(eventPackage + eventType);
		}
	}

	private int saveSingleVendorEvent(UUID aggregateId, int currentVersion, DomainEvent event) {
		String aggregateType = determineAggregateType(event);
		try {
			String eventData = objectMapper.writeValueAsString(event);
			TracingContext tracingContext = event.getTracingContext();
			String traceId = tracingContext != null ? tracingContext.getTraceId() : null;
			String spanId = tracingContext != null ? tracingContext.getSpanId() : null;

			jdbcTemplate.update(
					"INSERT INTO event_store (event_id, aggregate_id, aggregate_type, event_type, " +
							"version, event_timestamp, event_data, trace_id, span_id) " +
							"VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)",
					event.getEventId(),
					aggregateId,
					aggregateType,
					event.getEventType(),
					currentVersion + 1,
					Timestamp.from(event.getTimestamp()),
					eventData,
					traceId,
					spanId
			);

			log.debug("Saved vendor event {} for aggregate {} with traceId {}, spanId {}",
					event.getEventType(), aggregateId, traceId, spanId);
			return currentVersion + 1;
		} catch (JsonProcessingException e) {
			log.error("Error serializing vendor event: {}", e.getMessage(), e);
			throw new EventStoreException("Error saving vendor event to event store", e);
		} catch (Exception e) {
			log.error("Error saving vendor event: {}", e.getMessage(), e);
			throw new EventStoreException("Error saving vendor event to event store", e);
		}
	}

	private String determineAggregateType(DomainEvent event) {
		String eventClassName = event.getClass().getSimpleName();
		if (eventClassName.startsWith("Vendor")) {
			return "Vendor";
		}

		return eventClassName.replace("Event", "");
	}

	private Mono<Void> publishEventToKafka(DomainEvent event) {
		return publisher.publish(event, event.getAggregateId().toString())
				.doOnError(ex ->
						log.error("Failed to send event to Kafka: type={}, aggregateId={}, error={}",
								event.getClass().getSimpleName(), event.getAggregateId(), ex.getMessage(), ex)
				);
	}
}