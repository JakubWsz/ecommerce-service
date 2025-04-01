package pl.ecommerce.commons.kafka.dlq;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

@Slf4j
public abstract class DeadLetterQueueHandler {

	private static final int DEFAULT_MAX_RETRY_ATTEMPTS = 5;

	protected int getMaxRetryAttempts() {
		return DEFAULT_MAX_RETRY_ATTEMPTS;
	}

	@KafkaListener(
			topics = "#{{'${spring.application.name}.dlt.topics'.split(',')}}",
			groupId = "${spring.application.name}-dlq-processor",
			containerFactory = "deadLetterQueueListenerContainerFactory"
	)
	public void processDlqMessage(
			@Payload ConsumerRecord<String, String> record,
			@Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
			@Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage,
			Acknowledgment ack) {

		String messageKey = record.key() != null ? record.key() : "unknown";
		String originalTopic = topic.replace(".DLT", "");
		String messageId = originalTopic + "-" + messageKey + "-" + record.partition() + "-" + record.offset();

		try {
			log.warn("Processing DLQ message: {}, original topic: {}, exception: {}",
					messageId, originalTopic, exceptionMessage);

			DlqMessageStatus status = determineMessageStatus(record, exceptionMessage);
			storeFailedMessage(record, originalTopic, exceptionMessage, status);

			ack.acknowledge();

		} catch (Exception e) {
			log.error("Error processing DLQ message {}: {}", messageId, e.getMessage(), e);
		}
	}

	protected DlqMessageStatus determineMessageStatus(ConsumerRecord<String, String> record, String exceptionMessage) {
		return DlqMessageStatus.PENDING_RETRY;
	}

	protected abstract void storeFailedMessage(
			ConsumerRecord<String, String> record,
			String originalTopic,
			String errorMessage,
			DlqMessageStatus status);

	public abstract void processRetries();
}