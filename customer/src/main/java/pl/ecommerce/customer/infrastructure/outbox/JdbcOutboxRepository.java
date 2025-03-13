package pl.ecommerce.customer.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.customer.infrastructure.exception.EventStoreException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementacja repozytorium outbox dla niezawodnej publikacji zdarzeń
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class JdbcOutboxRepository implements OutboxRepository {

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	/**
	 * Zapisuje zdarzenie w tabeli outbox
	 */
	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void save(DomainEvent event) {
		try {
			String eventData = objectMapper.writeValueAsString(event);

			jdbcTemplate.update(
					"INSERT INTO event_outbox (id, aggregate_id, aggregate_type, event_type, event_data, timestamp) " +
							"VALUES (?, ?, ?, ?, ?::jsonb, ?)",
					UUID.randomUUID(),
					event.getAggregateId(),
					determineAggregateType(event),
					event.getClass().getSimpleName(),
					eventData,
					Timestamp.from(Instant.now())
			);
		} catch (JsonProcessingException e) {
			log.error("Error serializing event: {}", e.getMessage(), e);
			throw new EventStoreException("Error saving event to outbox", e);
		}
	}

	/**
	 * Pobiera nieprzetworzony zdarzenia
	 */
	@Override
	@Transactional(readOnly = true)
	public List<OutboxMessage> findUnprocessedMessages(int limit) {
		return jdbcTemplate.query(
				"SELECT * FROM event_outbox WHERE processed = false " +
						"ORDER BY timestamp ASC LIMIT ?",
				(rs, rowNum) -> {
					OutboxMessage message = new OutboxMessage();
					message.setId(UUID.fromString(rs.getString("id")));
					message.setAggregateId(UUID.fromString(rs.getString("aggregate_id")));
					message.setAggregateType(rs.getString("aggregate_type"));
					message.setEventType(rs.getString("event_type"));
					message.setEventData(rs.getString("event_data"));
					message.setTimestamp(rs.getTimestamp("timestamp").toInstant());
					message.setProcessed(rs.getBoolean("processed"));
					message.setProcessingAttempts(rs.getInt("processing_attempts"));

					Timestamp lastAttemptTimestamp = rs.getTimestamp("last_attempt_timestamp");
					if (lastAttemptTimestamp != null) {
						message.setLastAttemptTimestamp(lastAttemptTimestamp.toInstant());
					}

					message.setErrorMessage(rs.getString("error_message"));
					return message;
				},
				limit
		);
	}

	/**
	 * Oznacza wiadomość jako przetworzoną
	 */
	@Override
	@Transactional
	public void markAsProcessed(UUID messageId) {
		jdbcTemplate.update(
				"UPDATE event_outbox SET processed = true, last_attempt_timestamp = ? WHERE id = ?",
				Timestamp.from(Instant.now()),
				messageId
		);
	}

	/**
	 * Inkrementuje licznik prób przetwarzania i zapisuje błąd
	 */
	@Override
	@Transactional
	public void incrementProcessingAttempts(UUID messageId, String errorMessage) {
		jdbcTemplate.update(
				"UPDATE event_outbox SET processing_attempts = processing_attempts + 1, " +
						"last_attempt_timestamp = ?, error_message = ? WHERE id = ?",
				Timestamp.from(Instant.now()),
				errorMessage,
				messageId
		);
	}

	/**
	 * Usuwa przetworzone wiadomości starsze niż podany okres
	 */
	@Override
	@Transactional
	public int deleteProcessedMessagesBefore(Instant timestamp) {
		return jdbcTemplate.update(
				"DELETE FROM event_outbox WHERE processed = true AND timestamp < ?",
				Timestamp.from(timestamp)
		);
	}

	private String determineAggregateType(DomainEvent event) {
		// Określenie typu agregatu na podstawie zdarzenia
		String eventClassName = event.getClass().getSimpleName();
		if (eventClassName.startsWith("Customer")) {
			return "Customer";
		} else if (eventClassName.startsWith("Product")) {
			return "Product";
		} else if (eventClassName.startsWith("Order")) {
			return "Order";
		} else if (eventClassName.startsWith("Vendor")) {
			return "Vendor";
		}

		// Domyślnie użyj nazwy klasy bez "Event"
		return eventClassName.replace("Event", "");
	}
}