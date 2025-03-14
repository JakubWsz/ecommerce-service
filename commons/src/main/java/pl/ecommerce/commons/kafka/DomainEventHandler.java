package pl.ecommerce.commons.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import pl.ecommerce.commons.event.DomainEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public abstract class DomainEventHandler {

	protected final ObjectMapper objectMapper;
	protected final TopicsProvider topicsProvider;

	private final Map<Class<? extends DomainEvent>, Method> handlerMethods = new HashMap<>();

	@PostConstruct
	public void init() {
		for (Method method : this.getClass().getDeclaredMethods()) {
			if (method.isAnnotationPresent(EventHandler.class)) {
				Class<?>[] paramTypes = method.getParameterTypes();
				if ((paramTypes.length == 1 && DomainEvent.class.isAssignableFrom(paramTypes[0])) ||
						(paramTypes.length == 2 && DomainEvent.class.isAssignableFrom(paramTypes[0]) &&
								Map.class.isAssignableFrom(paramTypes[1]))) {
					@SuppressWarnings("unchecked")
					Class<? extends DomainEvent> eventType = (Class<? extends DomainEvent>) paramTypes[0];
					handlerMethods.put(eventType, method);
					log.info("Registered handler for event type: {}", eventType.getSimpleName());
				}
			}
		}
	}

	public String[] getSubscribedTopics() {
		return topicsProvider.getTopics();
	}

	public boolean processEvent(DomainEvent event, Map<String, String> headers) {
		Method handler = handlerMethods.get(event.getClass());
		if (handler != null) {
			try {
				if (handler.getParameterCount() == 1) {
					handler.invoke(this, event);
				} else {
					handler.invoke(this, event, headers);
				}
				return true;
			} catch (Exception e) {
				log.error("Error invoking handler for event {}: {}",
						event.getClass().getSimpleName(), e.getMessage(), e);
			}
		}
		return false;
	}

	@KafkaListener(
			topics = "#{@topicsProvider.getTopics()}",
			groupId = "${event.listener.group-id:${spring.application.name}-group}",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void consume(ConsumerRecord<String, DomainEvent> record, Acknowledgment ack) {
		DomainEvent event = record.value();

		Map<String, String> headers = new HashMap<>();
		for (Header header : record.headers()) {
			String headerValue = header.value() != null ? new String(header.value()) : null;
			headers.put(header.key(), headerValue);
		}

		String key = record.key();
		String eventType = event.getClass().getSimpleName();

		log.info("Received event: {} with key: {}, aggregateId: {}",
				eventType, key, event.getAggregateId());

		try {
			boolean processed = processEvent(event, headers);
			if (!processed) {
				log.info("No handler found for event type: {}", eventType);
			}
			ack.acknowledge();
		} catch (Exception e) {
			log.error("Error processing event: {}", eventType, e);
		}
	}
}