//package pl.ecommerce.commons.kafka;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.core.KafkaTemplate;
//import pl.ecommerce.commons.event.DomainEvent;
//
//import java.util.Arrays;
//
//@Slf4j
//@RequiredArgsConstructor
//public abstract class EventListener {
//
//	protected final ObjectMapper objectMapper;
//	protected final KafkaTemplate<String, Object> kafkaTemplate;
//	protected final TopicsProvider topicsProvider;
//
//	@Value("${event.listener.group-id:}")
//	private String groupId;
//
//	@Value("${spring.application.name:unknown-service}")
//	private String serviceName;
//
//	@PostConstruct
//	public void init() {
//		log.info("Initialized UniversalEventListener for service: {}", serviceName);
//		String[] topics = topicsProvider.getTopics();
//		if (topics.length > 0) {
//			log.info("Configured to listen on topics: {}", Arrays.toString(topics));
//		} else {
//			log.warn("No topics configured. Please set event.listener.topics property.");
//		}
//
//		if (groupId == null || groupId.isEmpty()) {
//			log.warn("No consumer group ID configured. Using default derived from service name.");
//		}
//	}
//
//	protected abstract boolean processEvent(DomainEvent event);
//
//	protected void forwardEvent(DomainEvent event, String targetTopic) {
//		try {
//			log.info("Forwarding event {} to topic: {}",
//					event.getClass().getSimpleName(), targetTopic);
//			kafkaTemplate.send(targetTopic, event)
//					.whenComplete((result, ex) -> {
//						if (ex == null) {
//							log.info("Forwarded event: {} to topic: {}",
//									event.getClass().getSimpleName(), targetTopic);
//						} else {
//							log.error("Failed to forward event: {} to topic: {}",
//									event.getClass().getSimpleName(), targetTopic, ex);
//						}
//					});
//		} catch (Exception e) {
//			log.error("Error forwarding event {} to topic {}: {}",
//					event.getClass().getSimpleName(), targetTopic, e.getMessage(), e);
//		}
//	}
//}
