package pl.ecommerce.customer.infrastructure.outbox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxMessage {
	private UUID id;
	private UUID aggregateId;
	private String aggregateType;
	private String eventType;
	private String eventData;
	private Instant timestamp;
	private boolean processed;
	private int processingAttempts;
	private Instant lastAttemptTimestamp;
	private String errorMessage;
}
