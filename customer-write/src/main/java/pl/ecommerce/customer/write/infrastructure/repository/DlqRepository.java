package pl.ecommerce.customer.write.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.kafka.dlq.DlqMessageStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DlqRepository {

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	@Transactional
	public void storeFailedMessage(ConsumerRecord<String, String> record,
								   String originalTopic,
								   String errorMessage,
								   DlqMessageStatus status) {
		try {
			String messageId = UUID.randomUUID().toString();
			String payload = record.value();
			String key = record.key();
			Map<String, String> headers = extractHeaders(record);
			String headerJson = objectMapper.writeValueAsString(headers);

			jdbcTemplate.update(
					"INSERT INTO dead_letter_queue " +
							"(message_id, original_topic, message_key, payload, error_message, headers, status, " +
							"retry_count, created_at, updated_at) " +
							"VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)",
					messageId, originalTopic, key, payload, errorMessage, headerJson, status.name(),
					0, Instant.now(), Instant.now()
			);

			log.info("Stored failed message in DLQ repository: {}, topic: {}, status: {}",
					messageId, originalTopic, status);

		} catch (Exception e) {
			log.error("Failed to store message in DLQ repository: {}", e.getMessage(), e);
		}
	}

	@Transactional
	public void updateMessageStatus(String messageId,
									DlqMessageStatus status,
									String notes) {
		try {
			jdbcTemplate.update(
					"UPDATE dead_letter_queue SET status = ?, notes = ?, updated_at = ? WHERE message_id = ?",
					status.name(), notes, Instant.now(), messageId
			);

			if (status == DlqMessageStatus.RETRY_IN_PROGRESS) {
				int retryCount = jdbcTemplate.queryForObject(
						"SELECT retry_count FROM dead_letter_queue WHERE message_id = ?",
						Integer.class,
						messageId);

				jdbcTemplate.update(
						"INSERT INTO dlq_retry_history (message_id, attempt_number, retry_time, result) " +
								"VALUES (?, ?, ?, ?)",
						messageId, retryCount + 1, Instant.now(), "STARTED");

				jdbcTemplate.update(
						"UPDATE dead_letter_queue SET retry_count = retry_count + 1 WHERE message_id = ?",
						messageId);
			}

			if (status == DlqMessageStatus.RETRY_SUCCEEDED || status == DlqMessageStatus.FAILED_PERMANENTLY) {
				jdbcTemplate.update(
						"UPDATE dlq_retry_history SET result = ?, error_message = ? " +
								"WHERE message_id = ? AND result = 'STARTED'",
						status.name(), notes, messageId);
			}

		} catch (Exception e) {
			log.error("Failed to update message status: {}", e.getMessage(), e);
		}
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> getPendingMessages(int limit) {
		return jdbcTemplate.queryForList(
				"SELECT * FROM dead_letter_queue " +
						"WHERE status = ? " +
						"ORDER BY created_at " +
						"LIMIT ?",
				DlqMessageStatus.PENDING_RETRY.name(), limit
		);
	}

	@Transactional(readOnly = true)
	public Map<String, Integer> getMessageCountByStatus() {
		List<Map<String, Object>> results = jdbcTemplate.queryForList(
				"SELECT status, COUNT(*) as count FROM dead_letter_queue GROUP BY status"
		);

		return results.stream()
				.collect(java.util.stream.Collectors.toMap(
						row -> (String) row.get("status"),
						row -> ((Number) row.get("count")).intValue()
				));
	}

	private Map<String, String> extractHeaders(ConsumerRecord<?, ?> record) {
		Map<String, String> result = new HashMap<>();

		for (Header header : record.headers()) {
			if (header.key() != null && header.value() != null) {
				String headerValue = new String(header.value(), StandardCharsets.UTF_8);
				result.put(header.key(), headerValue);
			}
		}

		return result;
	}
}