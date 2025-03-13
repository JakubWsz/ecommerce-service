package pl.ecommerce.customer.infrastructure.eventstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.customer.*;
import pl.ecommerce.customer.infrastructure.exception.ConcurrencyException;
import pl.ecommerce.customer.infrastructure.exception.EventStoreException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementacja EventStore wykorzystująca bazę danych poprzez JDBC.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JdbcEventStore implements EventStore {

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	/**
	 * Zapisuje listę zdarzeń dla agregatu
	 */
	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void saveEvents(UUID aggregateId, List<DomainEvent> events, int expectedVersion) {
		// Optymistyczne blokowanie - sprawdź aktualną wersję
		int currentVersion = getCurrentVersion(aggregateId);
		if (expectedVersion != -1 && currentVersion != expectedVersion) {
			throw new ConcurrencyException(
					String.format("Concurrent modification detected for aggregate %s. Expected version: %d, Actual version: %d",
							aggregateId, expectedVersion, currentVersion));
		}

		// Zapisz każde zdarzenie
		for (DomainEvent event : events) {
			// Ustal typ agregatu na podstawie zdarzenia
			String aggregateType = determineAggregateType(event);

			try {
				// Serializuj zdarzenie do JSON
				String eventData = objectMapper.writeValueAsString(event);

				// Zapisz zdarzenie w bazie danych
				jdbcTemplate.update(
						"INSERT INTO event_store (event_id, aggregate_id, aggregate_type, event_type, version, " +
								"timestamp, event_data, metadata) " +
								"VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)",
						event.getEventId(),
						aggregateId,
						aggregateType,
						event.getClass().getSimpleName(),
						currentVersion + 1,
						Timestamp.from(event.getTimestamp()),
						eventData,
						"{}"
				);

				currentVersion++;
			} catch (JsonProcessingException e) {
				log.error("Error serializing event: {}", e.getMessage(), e);
				throw new EventStoreException("Error saving event to event store", e);
			} catch (Exception e) {
				log.error("Error saving event: {}", e.getMessage(), e);
				throw new EventStoreException("Error saving event to event store", e);
			}
		}
	}

	/**
	 * Pobiera wszystkie zdarzenia dla agregatu
	 */
	@Override
	@Transactional(readOnly = true)
	public List<DomainEvent> getEventsForAggregate(UUID aggregateId) {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT * FROM event_store WHERE aggregate_id = ? AND deleted = false ORDER BY version ASC",
				aggregateId
		);

		List<DomainEvent> events = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			try {
				String eventType = (String) row.get("event_type");
				String eventData = (String) row.get("event_data");

				// Deserializuj zdarzenie w zależności od typu
				DomainEvent event = deserializeEvent(eventType, eventData);
				events.add(event);
			} catch (Exception e) {
				log.error("Error deserializing event: {}", e.getMessage(), e);
				throw new EventStoreException("Error loading events from event store", e);
			}
		}

		return events;
	}

	/**
	 * Oznacza zdarzenia dla agregatu jako usunięte
	 */
	@Override
	@Transactional
	public void markEventsAsDeleted(UUID aggregateId) {
		jdbcTemplate.update(
				"UPDATE event_store SET deleted = true WHERE aggregate_id = ?",
				aggregateId
		);
	}

	/**
	 * Pobiera aktualną wersję agregatu
	 */
	@Override
	@Transactional(readOnly = true)
	public int getCurrentVersion(UUID aggregateId) {
		Integer version = jdbcTemplate.query(
				"SELECT MAX(version) FROM event_store WHERE aggregate_id = ? AND deleted = false",
				new Object[]{aggregateId},
				(rs, rowNum) -> rs.getObject(1, Integer.class)
		).stream().findFirst().orElse(null);

		return version != null ? version : -1;
	}

	/**
	 * Określa typ agregatu na podstawie zdarzenia
	 */
	private String determineAggregateType(DomainEvent event) {
		if (event instanceof CustomerRegisteredEvent ||
				event instanceof CustomerUpdatedEvent ||
				event instanceof CustomerDeletedEvent ||
				event instanceof CustomerEmailChangedEvent ||
				event instanceof CustomerEmailVerifiedEvent ||
				event instanceof CustomerPhoneVerifiedEvent ||
				event instanceof CustomerAddressAddedEvent ||
				event instanceof CustomerAddressUpdatedEvent ||
				event instanceof CustomerAddressRemovedEvent ||
				event instanceof CustomerPreferencesUpdatedEvent ||
				event instanceof CustomerDeactivatedEvent ||
				event instanceof CustomerReactivatedEvent) {
			return "Customer";
		}

		return event.getClass().getSimpleName().replace("Event", "");
	}

	/**
	 * Deserializuje zdarzenie na podstawie typu i danych JSON
	 */
	private DomainEvent deserializeEvent(String eventType, String eventData) throws JsonProcessingException, ClassNotFoundException {
		// Określ klasę na podstawie typu zdarzenia
		Class<?> eventClass;

		try {
			// Najpierw spróbuj znaleźć w pakiecie customer.domain.events
			eventClass = Class.forName("pl.ecommerce.customer.domain.events." + eventType);
		} catch (ClassNotFoundException e) {
			// Jeśli nie ma w customer.domain.events, spróbuj w commons.event
			try {
				eventClass = Class.forName("pl.ecommerce.commons.event.customer." + eventType);
			} catch (ClassNotFoundException ex) {
				throw new EventStoreException("Could not find event class for type: " + eventType);
			}
		}

		// Deserializuj zdarzenie
		return (DomainEvent) objectMapper.readValue(eventData, eventClass);
	}
}
