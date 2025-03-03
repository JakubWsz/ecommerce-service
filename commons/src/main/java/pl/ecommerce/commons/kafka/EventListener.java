package pl.ecommerce.commons.kafka;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.GenericTypeResolver;
import pl.ecommerce.commons.event.DomainEvent;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public abstract class EventListener<T extends DomainEvent> {

	private final GenericKafkaEventListener kafkaEventListener;

	@PostConstruct
	@SuppressWarnings("unchecked")
	public void init() {
		Class<T> eventType = (Class<T>) GenericTypeResolver.resolveTypeArgument(
				getClass(), EventListener.class);
		kafkaEventListener.registerEventHandler(eventType, this::handleEvent);
	}

	protected abstract void handleEvent(T event);

	public List<String> getTopics() {
		return Collections.emptyList();
	}
}