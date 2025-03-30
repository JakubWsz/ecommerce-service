package pl.ecommerce.commons.kafka.dlq;

import java.time.Instant;
import java.util.Map;

public interface DeadLetterQueueMessage {

	String getMessageId();

	String getOriginalTopic();

	String getMessageKey();

	String getPayload();

	String getErrorMessage();

	DlqMessageStatus getStatus();

	int getRetryCount();

	Instant getCreatedAt();

	Instant getUpdatedAt();

	Map<String, String> getHeaders();
}