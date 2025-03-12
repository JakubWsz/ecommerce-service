package pl.ecommerce.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.kafka.EventListener;

@Component
@Slf4j
public class EventStoreListener extends EventListener {

	private final EventStoreService eventStoreService;

	public EventStoreListener(ObjectMapper objectMapper, KafkaTemplate<String, Object> kafkaTemplate,
							  EventStoreService eventStoreService) {
		super(objectMapper, kafkaTemplate);
		this.eventStoreService = eventStoreService;
	}

	@Override
	protected boolean processEvent(DomainEvent event) {
		log.info("EventStore saving event: {} with correlationId: {}",
				event.getClass().getSimpleName(), event.getCorrelationId());

		try {
			eventStoreService.saveEvent(event);
			return true;
		} catch (Exception e) {
			log.error("Failed to save event to event store: {}", e.getMessage(), e);
			return false;
		}
	}
}
