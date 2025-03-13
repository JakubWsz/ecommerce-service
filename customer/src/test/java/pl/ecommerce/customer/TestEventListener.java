//package pl.ecommerce.customer;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Primary;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Component;
//import pl.ecommerce.commons.event.DomainEvent;
//import pl.ecommerce.commons.kafka.DomainEventHandler;
//import pl.ecommerce.commons.kafka.EventHandler;
//import pl.ecommerce.commons.kafka.TopicsProvider;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//
//@Slf4j
//@Component
//@Primary
//public class TestEventListener extends DomainEventHandler {
//
//	private final ConcurrentMap<Class<? extends DomainEvent>, List<DomainEvent>> capturedEvents = new ConcurrentHashMap<>();
//
//	public TestEventListener(ObjectMapper objectMapper, KafkaTemplate<String, Object> kafkaTemplate, TopicsProvider topicsProvider) {
//		super(objectMapper, kafkaTemplate, topicsProvider);
//	}
//
////	@KafkaListener(
////			topics = "#{@topicsProvider.getTopics()}",
////			groupId = "${event.listener.group-id:${spring.application.name}-group}",
////			containerFactory = "kafkaListenerContainerFactory"
////	)
////	public void consume(DomainEvent event) {
////		log.info("Test listener captured event: {} with correlationId: {}",
////				event.getClass().getSimpleName(), event.getCorrelationId());
////		capturedEvents
////				.computeIfAbsent(event.getClass(), k -> Collections.synchronizedList(new ArrayList<>()))
////				.add(event);
////		System.out.println(capturedEvents);
////	}
//
//
//	@EventHandler
//	public void handle(CustomerRegisteredEvent event) {
//		log.info("Processing CustomerRegisteredEvent: {}", event.getCustomerId());
//		capturedEvents.computeIfAbsent(event.getClass(),k -> Collections.synchronizedList(new ArrayList<>()))
//				.add(event);
//	}
//
//	@EventHandler
//	public void handle(CustomerDeletedEvent event) {
//		log.info("Processing CustomerDeletedEvent: {}", event.getCustomerId());
//		capturedEvents.computeIfAbsent(event.getClass(),k -> Collections.synchronizedList(new ArrayList<>()))
//				.add(event);
//	}
//
//	@EventHandler
//	public void handle(CustomerUpdatedEvent event) {
//		log.info("Processing CustomerUpdatedEvent: {}", event.getCustomerId());
//		capturedEvents.computeIfAbsent(event.getClass(),k -> Collections.synchronizedList(new ArrayList<>()))
//				.add(event);
//	}
//
//
//
//	public <T extends DomainEvent> List<T> getCapturedEvents(Class<T> eventType) {
//		List<DomainEvent> events = capturedEvents.getOrDefault(eventType, Collections.emptyList());
//		@SuppressWarnings("unchecked")
//		List<T> typedEvents = events.stream()
//				.map(event -> (T) event)
//				.toList();
//		return typedEvents;
//	}
//
//	public <T extends DomainEvent> T getCapturedEvent(Class<T> eventType) {
//		List<DomainEvent> events = capturedEvents.getOrDefault(eventType, Collections.emptyList());
//		@SuppressWarnings("unchecked")
//		var event = events.stream()
//				.map(e -> (T) e)
//				.findFirst()
//				.orElseThrow();
//		return event;
//	}
//
//	public boolean wasEventReceivedForCustomer(Class<? extends DomainEvent> eventType, UUID customerId) {
//		List<DomainEvent> events = capturedEvents.getOrDefault(eventType, Collections.emptyList());
//		return events.stream().anyMatch(event -> {
//			if (event instanceof CustomerRegisteredEvent customerEvent) {
//				return customerEvent.getCustomerId().equals(customerId);
//			} else if (event instanceof CustomerUpdatedEvent customerEvent) {
//				return customerEvent.getCustomerId().equals(customerId);
//			} else if (event instanceof CustomerDeletedEvent customerEvent) {
//				return customerEvent.getCustomerId().equals(customerId);
//			}
//			return false;
//		});
//	}
//
//	public void clearEvents() {
//		capturedEvents.clear();
//	}
//
//	public int getEventCount(Class<? extends DomainEvent> eventType) {
//		return capturedEvents.getOrDefault(eventType, Collections.emptyList()).size();
//	}
//}
