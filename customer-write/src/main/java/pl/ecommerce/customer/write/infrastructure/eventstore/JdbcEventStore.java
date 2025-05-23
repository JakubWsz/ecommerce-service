package pl.ecommerce.customer.write.infrastructure.eventstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.event.AbstractDomainEvent;
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
	public void saveEvents(UUID aggregateId, List<AbstractDomainEvent> events, int expectedVersion) {
		int currentVersion = getCurrentVersion(aggregateId);

		if (expectedVersion != -1 && currentVersion != expectedVersion) {
			throw new ConcurrencyException(String.format(
					"Concurrent modification detected for aggregate %s. Expected version: %d, Actual version: %d",
					aggregateId, expectedVersion, currentVersion));
		}

		for (AbstractDomainEvent event : events) {
			currentVersion = saveSingleEvent(aggregateId, currentVersion, event);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<AbstractDomainEvent> getEventsForAggregate(UUID aggregateId) {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT * FROM event_store WHERE aggregate_id = ? AND deleted = false ORDER BY version ASC",
				aggregateId
		);

		List<AbstractDomainEvent> events = new ArrayList<>();
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

	private int saveSingleEvent(UUID aggregateId, int currentVersion, AbstractDomainEvent event) {
		String aggregateType = determineAggregateType(event);
		try {
			String eventData = objectMapper.writeValueAsString(event);
			jdbcTemplate.update(
					"INSERT INTO event_store (event_id, aggregate_id, aggregate_type, event_type, " +
							"version, event_timestamp, event_data) " +
							"VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)",
					event.getEventId(),
					aggregateId,
					aggregateType,
					event.getEventType(),
					currentVersion + 1,
					Timestamp.from(event.getTimestamp()),
					eventData
			);

			log.debug("Saved event {} for aggregate {}",
					event.getEventType(), aggregateId);
			return currentVersion + 1;
		} catch (JsonProcessingException e) {
			log.error("Error serializing event: {}", e.getMessage(), e);
			throw new EventStoreException("Error saving event to event store", e);
		} catch (Exception e) {
			log.error("Error saving event: {}", e.getMessage(), e);
			throw new EventStoreException("Error saving event to event store", e);
		}
	}

	private String determineAggregateType(AbstractDomainEvent event) {
		String eventClassName = event.getClass().getSimpleName();
		if (eventClassName.startsWith("Customer")) {
			return "Customer";
		}

		return eventClassName.replace("Event", "");
	}

	private AbstractDomainEvent processEventRow(Map<String, Object> row) {
		try {
			String eventType = (String) row.get("event_type");
			Object eventDataObj = row.get("event_data");
			String eventData;

			if (Objects.nonNull(eventDataObj)) {
				if (eventDataObj instanceof PGobject pgObject) {
					eventData = pgObject.getValue();
				} else if (eventDataObj instanceof String) {
					eventData = (String) eventDataObj;
				} else {
					eventData = eventDataObj.toString();
				}
			} else {
				eventData = null;
			}

			return deserializeEvent(eventType, eventData);
		} catch (Exception e) {
			log.error("Error deserializing event: {}", e.getMessage(), e);
			throw new EventStoreException("Error loading events from event store", e);
		}
	}

	private AbstractDomainEvent deserializeEvent(String eventType, String eventData) throws JsonProcessingException {
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

		return (AbstractDomainEvent) objectMapper.readValue(eventData, eventClass);
	}
}