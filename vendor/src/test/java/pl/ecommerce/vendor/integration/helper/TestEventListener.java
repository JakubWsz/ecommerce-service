package pl.ecommerce.vendor.integration.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import pl.ecommerce.commons.event.vendor.*;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.kafka.DomainEventHandler;
import pl.ecommerce.commons.kafka.EventHandler;
import pl.ecommerce.commons.kafka.TopicsProvider;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@Primary
@Profile("test")
public class TestEventListener extends DomainEventHandler {

	private final ConcurrentMap<Class<? extends DomainEvent>, List<DomainEvent>> capturedEvents = new ConcurrentHashMap<>();

	public TestEventListener(ObjectMapper objectMapper, KafkaTemplate<String, Object> kafkaTemplate, TopicsProvider topicsProvider) {
		super(objectMapper, kafkaTemplate, topicsProvider);
	}

//	@KafkaListener(
//			topics = "#{@topicsProvider.getTopics()}",
//			groupId = "${event.listener.group-id:${spring.application.name}-group}",
//			containerFactory = "kafkaListenerContainerFactory"
//	)
//	public void consume(DomainEvent event) {
//		log.info("Test listener captured event: {} with correlationId: {}",
//				event.getClass().getSimpleName(), event.getCorrelationId());
//
//		System.out.println("TestEventListener captured: " + event.getClass().getSimpleName());
//		System.out.println(Arrays.toString(getSubscribedTopics()));
//
//		capturedEvents
//				.computeIfAbsent(event.getClass(), k -> Collections.synchronizedList(new ArrayList<>()))
//				.add(event);
//		System.out.println(capturedEvents);
//	}


	@EventHandler
	public void handleVendorRegisteredEvent(VendorRegisteredEvent event) {
		log.info("Processing VendorRegisteredEvent: {}", event.getVendorId());
		capturedEvents.computeIfAbsent(event.getClass(),k -> Collections.synchronizedList(new ArrayList<>()))
				.add(event);
	}
	@EventHandler
	public void handleVendorRegisteredEvent(VendorPaymentProcessedEvent event) {
		log.info("Processing VendorPaymentProcessedEvent: {}", event.getVendorId());
		capturedEvents.computeIfAbsent(event.getClass(),k -> Collections.synchronizedList(new ArrayList<>()))
				.add(event);
	}
	@EventHandler
	public void handleVendorRegisteredEvent(VendorCategoriesAssignedEvent event) {
		log.info("Processing VendorCategoriesAssignedEvent: {}", event.getVendorId());
		capturedEvents.computeIfAbsent(event.getClass(),k -> Collections.synchronizedList(new ArrayList<>()))
				.add(event);
	}
	@EventHandler
	public void handleVendorRegisteredEvent(VendorStatusChangedEvent event) {
		log.info("Processing VendorStatusChangedEvent: {}", event.getVendorId());
		capturedEvents.computeIfAbsent(event.getClass(),k -> Collections.synchronizedList(new ArrayList<>()))
				.add(event);
	}
	@EventHandler
	public void handleVendorRegisteredEvent(VendorUpdatedEvent event) {
		log.info("Processing VendorUpdatedEvent: {}", event.getVendorId());
		capturedEvents.computeIfAbsent(event.getClass(),k -> Collections.synchronizedList(new ArrayList<>()))
				.add(event);
	}
	@EventHandler
	public void handleVendorRegisteredEvent(VendorVerificationCompletedEvent event) {
		log.info("Processing VendorVerificationCompletedEvent: {}", event.getVendorId());
		capturedEvents.computeIfAbsent(event.getClass(),k -> Collections.synchronizedList(new ArrayList<>()))
				.add(event);
	}


	public <T extends DomainEvent> List<T> getCapturedEvents(Class<T> eventType) {
		List<DomainEvent> events = capturedEvents.getOrDefault(eventType, Collections.emptyList());
		@SuppressWarnings("unchecked")
		List<T> typedEvents = events.stream()
				.map(event -> (T) event)
				.toList();
		return typedEvents;
	}

	public <T extends DomainEvent> Flux<List<T>> getCapturedEventsFlux(Class<T> eventType) {
		List<DomainEvent> events = capturedEvents.getOrDefault(eventType, Collections.emptyList());
		@SuppressWarnings("unchecked")
		List<T> typedEvents = events.stream()
				.map(event -> (T) event)
				.toList();
		return Flux.just(typedEvents);
	}

	public <T extends DomainEvent> T getCapturedEvent(Class<T> eventType) {
		List<DomainEvent> events = capturedEvents.getOrDefault(eventType, Collections.emptyList());
		@SuppressWarnings("unchecked")
		var event = events.stream()
				.map(e -> (T) e)
				.findFirst()
				.orElseThrow();
		return event;
	}

	public boolean wasEventReceivedForCustomer(Class<? extends DomainEvent> eventType, UUID customerId) {
		List<DomainEvent> events = capturedEvents.getOrDefault(eventType, Collections.emptyList());
		return events.stream().anyMatch(event -> {
			if (event instanceof CustomerRegisteredEvent customerEvent) {
				return customerEvent.getCustomerId().equals(customerId);
			} else if (event instanceof CustomerUpdatedEvent customerEvent) {
				return customerEvent.getCustomerId().equals(customerId);
			} else if (event instanceof CustomerDeletedEvent customerEvent) {
				return customerEvent.getCustomerId().equals(customerId);
			}
			return false;
		});
	}

	public void clearEvents() {
		capturedEvents.clear();
	}

	public int getEventCount(Class<? extends DomainEvent> eventType) {
		return capturedEvents.getOrDefault(eventType, Collections.emptyList()).size();
	}

	public String[] getSubscribedTopics() {
		return topicsProvider.getTopics();
	}
}
