package pl.ecommerce.customer.write.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.kafka.dlq.DeadLetterQueueHandler;
import pl.ecommerce.commons.kafka.dlq.DlqMessageStatus;
import pl.ecommerce.commons.kafka.dlq.DlqMetrics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomerDeadLetterQueueHandler extends DeadLetterQueueHandler {

	private final pl.ecommerce.customer.write.infrastructure.event.DlqRepository dlqRepository;
	private final DlqMetrics dlqMetrics;
	private final Map<String, Integer> retryAttempts = new ConcurrentHashMap<>();

	@Override
	protected int getMaxRetryAttempts() {
		return 5;
	}

	@Override
	protected DlqMessageStatus determineMessageStatus(ConsumerRecord<String, String> record, String exceptionMessage) {
		String messageKey = record.key() != null ? record.key() : "unknown";
		String originalTopic = record.topic().replace(".DLT", "");
		String messageId = originalTopic + "-" + messageKey + "-" + record.partition() + "-" + record.offset();

		int retryCount = retryAttempts.getOrDefault(messageId, 0);
		retryAttempts.put(messageId, retryCount + 1);

		if (retryCount >= getMaxRetryAttempts()) {
			dlqMetrics.recordPermanentFailure();
			return DlqMessageStatus.FAILED_PERMANENTLY;
		}

		return DlqMessageStatus.PENDING_RETRY;
	}

	@Override
	protected void storeFailedMessage(
			ConsumerRecord<String, String> record,
			String originalTopic,
			String errorMessage,
			DlqMessageStatus status) {

		dlqMetrics.recordDlqMessage(originalTopic);
		dlqRepository.storeFailedMessage(record, originalTopic, errorMessage, status);
	}

	/**
	 * Scheduled job to process pending retry messages.
	 */
	@Override
	@Scheduled(fixedDelayString = "${kafka.dlq.retry-interval-ms:60000}")
	public void processRetries() {
		log.debug("Processing DLQ retries");

		var timer = dlqMetrics.startTimer();
		try {
			List<Map<String, Object>> pendingMessages = dlqRepository.getPendingMessages(20);
			if (pendingMessages.isEmpty()) {
				log.debug("No pending messages to retry");
				return;
			}

			dlqMetrics.updatePendingCount(pendingMessages.size());
			log.info("Found {} messages pending retry", pendingMessages.size());

			for (Map<String, Object> message : pendingMessages) {
				String messageId = (String) message.get("message_id");
				String originalTopic = (String) message.get("original_topic");
				String payload = (String) message.get("payload");
				String key = (String) message.get("message_key");

				try {
					dlqMetrics.recordRetryAttempt();
					dlqRepository.updateMessageStatus(
							messageId,
							DlqMessageStatus.RETRY_IN_PROGRESS,
							"Retry attempt in progress");

					dlqRepository.updateMessageStatus(
							messageId,
							DlqMessageStatus.RETRY_SUCCEEDED,
							"Retry successful");
					dlqMetrics.recordRetrySuccess();

				} catch (Exception e) {
					log.error("Error retrying message {}: {}", messageId, e.getMessage(), e);
					int retryCount = ((Number) message.get("retry_count")).intValue() + 1;

					if (retryCount >= getMaxRetryAttempts()) {
						dlqRepository.updateMessageStatus(
								messageId,
								DlqMessageStatus.FAILED_PERMANENTLY,
								"Max retry attempts exceeded: " + e.getMessage());
						dlqMetrics.recordPermanentFailure();
					} else {
						dlqRepository.updateMessageStatus(
								messageId,
								DlqMessageStatus.PENDING_RETRY,
								"Retry failed: " + e.getMessage());
					}
				}
			}
		} finally {
			dlqMetrics.stopTimer(timer);
		}
	}
}