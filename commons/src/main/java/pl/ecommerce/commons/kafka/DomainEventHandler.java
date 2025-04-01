package pl.ecommerce.commons.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.kafka.dlq.DlqMetrics;
import pl.ecommerce.commons.tracing.TracingContext;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public abstract class DomainEventHandler {

	protected final ObjectMapper objectMapper;
	protected final TopicsProvider topicsProvider;

	@Autowired(required = false)
	private DlqMetrics dlqMetrics;

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
	public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
		String traceId = "unknown";
		for (Header header : record.headers()) {
			if ("trace-id".equals(header.key())) {
				traceId = new String(header.value(), StandardCharsets.UTF_8);
				break;
			}
		}

		MDC.put("traceId", traceId);

		try {
			Object value = record.value();

			if (!(value instanceof DomainEvent event)) {
				log.error("Received message is not a DomainEvent: {}, traceId: {}", value, traceId);
				ack.acknowledge();
				return;
			}

			setTracingContextFromHeaders(record, event);

			String eventType = event.getClass().getSimpleName();
			String key = record.key();
			UUID aggregateId = event.getAggregateId();

			log.info("Received event: {} with key: {}, aggregateId: {}, traceId: {}",
					eventType, key, aggregateId, traceId);

			Map<String, String> headers = extractHeaders(record);

			boolean processed = processEvent(event, headers);
			if (!processed) {
				log.info("No handler found for event type: {}, traceId: {}", eventType, traceId);
			}

			ack.acknowledge();
		} catch (Exception e) {
			log.error("Error processing Kafka message, traceId: {}: {}", traceId, e.getMessage(), e);
			ack.acknowledge();
		} finally {
			MDC.remove("traceId");
		}
	}

	private void setTracingContextFromHeaders(ConsumerRecord<?, ?> record, DomainEvent event) {
		String traceId = null;
		String spanId = null;
		String userId = null;
		String sourceService = null;
		String sourceOperation = null;

		for (Header header : record.headers()) {
			switch (header.key()) {
				case "trace-id":
					traceId = new String(header.value(), StandardCharsets.UTF_8);
					break;
				case "span-id":
					spanId = new String(header.value(), StandardCharsets.UTF_8);
					break;
				case "user-id":
					userId = new String(header.value(), StandardCharsets.UTF_8);
					break;
				case "source-service":
					sourceService = new String(header.value(), StandardCharsets.UTF_8);
					break;
				case "source-operation":
					sourceOperation = new String(header.value(), StandardCharsets.UTF_8);
					break;
			}
		}

		if (traceId != null) {
			TracingContext tracingContext = TracingContext.builder()
					.traceId(traceId)
					.spanId(spanId)
					.userId(userId)
					.sourceService(sourceService)
					.sourceOperation(sourceOperation)
					.build();

			event.setTracingContext(tracingContext);
		}
	}

	private Map<String, String> extractHeaders(ConsumerRecord<?, ?> record) {
		Map<String, String> result = new HashMap<>();

		for (Header header : record.headers()) {
			if (Objects.nonNull(header.key()) && Objects.nonNull(header.value())) {
				String headerValue = new String(header.value(), StandardCharsets.UTF_8);
				result.put(header.key(), headerValue);
			}
		}

		return result;
	}
}