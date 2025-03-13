package pl.ecommerce.customer.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Procesor publikujący zdarzenia z tabeli outbox do Kafki
 */
@Component
@Slf4j
public class OutboxProcessor {

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ScheduledExecutorService executorService;
	private final long processingIntervalMs;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final int batchSize = 10;
	private final int maxAttempts = 5;
	private volatile boolean running = true;

	public OutboxProcessor(
			OutboxRepository outboxRepository,
			KafkaTemplate<String, Object> kafkaTemplate,
			ScheduledExecutorService executorService,
			long processingIntervalMs) {
		this.outboxRepository = outboxRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.executorService = executorService;
		this.processingIntervalMs = processingIntervalMs;
	}

	@PostConstruct
	public void init() {
		log.info("Starting Outbox Processor");
		executorService.scheduleWithFixedDelay(
				this::processOutbox,
				0,
				processingIntervalMs,
				TimeUnit.MILLISECONDS
		);
	}

	@PreDestroy
	public void shutdown() {
		log.info("Stopping Outbox Processor");
		running = false;
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			executorService.shutdownNow();
		}
	}

	private void processOutbox() {
		if (!running) {
			return;
		}

		try {
			log.debug("Processing outbox messages");
			List<OutboxMessage> messages = outboxRepository.findUnprocessedMessages(batchSize);

			for (OutboxMessage message : messages) {
				if (!running) {
					break;
				}

				try {
					if (message.getProcessingAttempts() >= maxAttempts) {
						log.warn("Message {} exceeded max processing attempts, marking as dead letter", message.getId());
						// Tu można dodać logikę zapisującą do kolejki martwych listów
						outboxRepository.markAsProcessed(message.getId());
						continue;
					}

					String eventType = message.getEventType();
					String topicName = determineTopicName(message);
					String eventData = message.getEventData();

					log.debug("Publishing event {} to topic {}", message.getId(), topicName);

					kafkaTemplate.send(new ProducerRecord<>(topicName, eventData))
							.whenComplete((result, ex) -> {
								if (ex == null) {
									outboxRepository.markAsProcessed(message.getId());
									log.debug("Successfully processed message: {}", message.getId());
								} else {
									String errorMessage = ex.getMessage();
									log.error("Failed to process message {}: {}", message.getId(), errorMessage, ex);
									outboxRepository.incrementProcessingAttempts(message.getId(), errorMessage);
								}
							});

				} catch (Exception e) {
					log.error("Error processing outbox message {}: {}", message.getId(), e.getMessage(), e);
					outboxRepository.incrementProcessingAttempts(message.getId(), e.getMessage());
				}
			}

			// Usuń przetworzone wiadomości starsze niż 7 dni
			if (!messages.isEmpty()) {
				int deleted = outboxRepository.deleteProcessedMessagesBefore(
						java.time.Instant.now().minus(java.time.Duration.ofDays(7))
				);
				if (deleted > 0) {
					log.info("Cleaned up {} old processed outbox messages", deleted);
				}
			}

		} catch (Exception e) {
			log.error("Error in outbox processor: {}", e.getMessage(), e);
		}
	}

	private String determineTopicName(OutboxMessage message) {
		try {
			// Sprawdź, czy eventData ma pole @Message, które określa nazwę tematu
			JsonNode eventData = objectMapper.readTree(message.getEventData());
			if (eventData.has("@Message")) {
				return eventData.get("@Message").asText();
			}

			// Jeśli nie, użyj domyślnej konwencji nazewnictwa
			String eventType = message.getEventType().toLowerCase().replace("event", "");
			String aggregateType = message.getAggregateType().toLowerCase();

			return aggregateType + "." + eventType;

		} catch (Exception e) {
			log.warn("Error determining topic name for message {}, using default: {}",
					message.getId(), message.getEventType(), e);
			return "default-events";
		}
	}
}