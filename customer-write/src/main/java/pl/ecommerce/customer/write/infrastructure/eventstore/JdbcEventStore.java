package pl.ecommerce.customer.write.infrastructure.eventstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.commons.tracing.TracingContextHolder;
import pl.ecommerce.customer.write.infrastructure.exception.ConcurrencyException;
import pl.ecommerce.customer.write.infrastructure.exception.EventStoreException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class JdbcEventStore implements EventStore {

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveEvents(UUID aggregateId, List<DomainEvent> events, int expectedVersion) {
		int currentVersion = getCurrentVersion(aggregateId);
		if (expectedVersion != -1 && currentVersion != expectedVersion) {
			throw new ConcurrencyException(String.format(
					"Concurrent modification detected for aggregate %s. Expected version: %d, Actual version: %d",
					aggregateId, expectedVersion, currentVersion));
		}

		for (DomainEvent event : events) {
			if (Objects.isNull(event.getTracingContext())) {
				TracingContext tracingContext = TracingContextHolder.getContext();
				if (Objects.nonNull(tracingContext)) {
					event.setTracingContext(tracingContext);
				}
			}

			currentVersion = saveSingleEvent(aggregateId, currentVersion, event);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<DomainEvent> getEventsForAggregate(UUID aggregateId) {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT * FROM event_store WHERE aggregate_id = ? AND deleted = false ORDER BY version ASC",
				aggregateId
		);

		List<DomainEvent> events = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			events.add(processEventRow(row));
		}
		return events;
	}

	@Override
	@Transactional
	public void markEventsAsDeleted(UUID aggregateId) {
		jdbcTemplate.update(
				"UPDATE event_store SET deleted = true WHERE aggregate_id = ?",
				aggregateId
		);
		log.info("Marked events for aggregate {} as deleted", aggregateId);
	}

	private int getCurrentVersion(UUID aggregateId) {
		Integer version = jdbcTemplate.queryForObject(
				"SELECT COALESCE(MAX(version), 0) FROM event_store WHERE aggregate_id = ? AND deleted = false",
				Integer.class,
				aggregateId);
		return Objects.nonNull(version) ? version : 0;
	}

	private int saveSingleEvent(UUID aggregateId, int currentVersion, DomainEvent event) {
		String aggregateType = determineAggregateType(event);
		try {
			String eventData = objectMapper.writeValueAsString(event);
			TracingContext tracingContext = event.getTracingContext();
			String traceId = Objects.nonNull(tracingContext) ? tracingContext.getTraceId() : null;
			String spanId = Objects.nonNull(tracingContext) ? tracingContext.getSpanId() : null;

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

			log.debug("Saved event {} for aggregate {} with traceId {}, spanId {}",
					event.getEventType(), aggregateId, traceId, spanId);
			return currentVersion + 1;
		} catch (JsonProcessingException e) {
			log.error("Error serializing event: {}", e.getMessage(), e);
			throw new EventStoreException("Error saving event to event store", e);
		} catch (Exception e) {
			log.error("Error saving event: {}", e.getMessage(), e);
			throw new EventStoreException("Error saving event to event store", e);
		}
	}

	private String determineAggregateType(DomainEvent event) {
		String eventClassName = event.getClass().getSimpleName();
		if (eventClassName.startsWith("Customer")) {
			return "Customer";
		}

		return eventClassName.replace("Event", "");
	}

	private DomainEvent processEventRow(Map<String, Object> row) {
		try {
			String eventType = (String) row.get("event_type");
			String eventData = (String) row.get("event_data");

			DomainEvent event = deserializeEvent(eventType, eventData);

			String traceId = (String) row.get("trace_id");
			String spanId = (String) row.get("span_id");

			if (Objects.nonNull(traceId)) {
				TracingContext tracingContext = TracingContext.builder()
						.traceId(traceId)
						.spanId(spanId)
						.sourceService("customer-write")
						.build();

				event.setTracingContext(tracingContext);

				log.debug("Loaded event {} with traceId {}, spanId {}",
						eventType, tracingContext.getTraceId(), tracingContext.getSpanId());
			}

			return event;
		} catch (Exception e) {
			log.error("Error deserializing event: {}", e.getMessage(), e);
			throw new EventStoreException("Error loading events from event store", e);
		}
	}

	private DomainEvent deserializeEvent(String eventType, String eventData) throws JsonProcessingException {
		Class<?> eventClass;

		try {
			eventClass = Class.forName("pl.ecommerce.commons.event.customer." + eventType);
		} catch (ClassNotFoundException e) {
			try {
				eventClass = Class.forName("pl.ecommerce.commons.event." + eventType);
			} catch (ClassNotFoundException ex) {
				throw new EventStoreException("Could not find event class for type: " + eventType);
			}
		}

		return (DomainEvent) objectMapper.readValue(eventData, eventClass);
	}
}