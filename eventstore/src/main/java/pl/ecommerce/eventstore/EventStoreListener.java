package pl.ecommerce.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.DomainEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventStoreListener {
	private final EventStoreService eventStoreService;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "#{'${eventstore.topics}'.split(',')}", groupId = "event-store-group")
	public void listen(ConsumerRecord<String, String> record) {
		log.info("Raw event received: {}", record.value());

		try {
			DomainEvent event = objectMapper.readValue(record.value(), DomainEvent.class);
			log.info("Parsed event: {} for correlationId: {}", event.getClass().getSimpleName(), event.getCorrelationId());
			eventStoreService.saveEvent(event);
		} catch (Exception e) {
			log.error("Failed to parse event", e);
		}
	}
}
