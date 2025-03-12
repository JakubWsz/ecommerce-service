package pl.ecommerce.commons.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import pl.ecommerce.commons.event.DomainEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public abstract class DomainEventHandler{

	protected final ObjectMapper objectMapper;
	protected final KafkaTemplate<String, Object> kafkaTemplate;
	protected final TopicsProvider topicsProvider;

	private final Map<Class<? extends DomainEvent>, Method> handlerMethods = new HashMap<>();

	@PostConstruct
	public void init() {
		for (Method method : this.getClass().getDeclaredMethods()) {
			if (method.isAnnotationPresent(EventHandler.class)) {
				Class<?>[] paramTypes = method.getParameterTypes();
				if (paramTypes.length == 1 && DomainEvent.class.isAssignableFrom(paramTypes[0])) {
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

	public boolean processEvent(DomainEvent event) {
		Method handler = handlerMethods.get(event.getClass());
		if (handler != null) {
			try {
				handler.invoke(this, event);
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
	public void consume(DomainEvent event) {
		log.info("Received event: {} with correlationId: {}",
				event.getClass().getSimpleName(), event.getCorrelationId());
		boolean processed = processEvent(event);
		if (!processed) {
			log.info("No handler found for event type: {}", event.getClass().getSimpleName());
		}
	}
}