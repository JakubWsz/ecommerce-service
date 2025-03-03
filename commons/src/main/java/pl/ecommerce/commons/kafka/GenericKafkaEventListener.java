package pl.ecommerce.commons.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.DomainEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenericKafkaEventListener {

	private final ObjectMapper objectMapper;

	private final Map<String, List<Consumer<DomainEvent>>> eventHandlers = new ConcurrentHashMap<>();

	public <T extends DomainEvent> void registerEventHandler(Class<T> eventType, Consumer<T> handler) {
		String eventTypeName = eventType.getSimpleName();
		eventHandlers.computeIfAbsent(eventTypeName, k -> new CopyOnWriteArrayList<>())
				.add(event -> {
					if (eventType.isInstance(event)) {
						handler.accept(eventType.cast(event));
					}
				});
		log.info("Registered handler for event type: {}", eventTypeName);
	}

	@KafkaListener(topicPattern = "${ecommerce.kafka.topics.pattern:.*-events}",
			groupId = "${ecommerce.kafka.consumer.group-id:default-group}")
	public void listen(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		try {
			JsonNode jsonNode = objectMapper.readTree(message);
			String eventType = jsonNode.get("eventType").asText();

			log.debug("Received event of type {} from topic {}", eventType, topic);

			DomainEvent event = objectMapper.readValue(message, DomainEvent.class);

			List<Consumer<DomainEvent>> handlers = eventHandlers.getOrDefault(eventType, Collections.emptyList());
			if (!handlers.isEmpty()) {
				handlers.forEach(handler -> handler.accept(event));
			} else {
				log.debug("No handlers registered for event type: {}", eventType);
			}
		} catch (Exception e) {
			log.error("Error processing Kafka message from topic {}: {}", topic, message, e);
		}
	}
}