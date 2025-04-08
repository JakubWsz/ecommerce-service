package pl.ecommerce.commons.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.kafka.dlq.DlqMetrics;
import pl.ecommerce.commons.tracing.KafkaTracingPropagator;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.nonNull;

@Slf4j
@RequiredArgsConstructor
public abstract class DomainEventHandler {

	protected final ObjectMapper objectMapper;
	protected final TopicsProvider topicsProvider;
	protected final String applicationName;

	@Autowired(required = false)
	private DlqMetrics dlqMetrics;

	private final Map<Class<? extends DomainEvent>, java.lang.reflect.Method> handlerMethods = new HashMap<>();

	// Getter – pozostały bez zmian
	private static final TextMapGetter<Headers> GETTER = new TextMapGetter<>() {
		@Override
		public Iterable<String> keys(Headers carrier) {
			Map<String, String> headerMap = new HashMap<>();
			for (Header header : carrier) {
				headerMap.put(header.key(), "");
			}
			return headerMap.keySet();
		}

		@Override
		public String get(Headers carrier, String key) {
			Header header = nonNull(carrier) ? carrier.lastHeader(key) : null;
			return nonNull(header) ? new String(header.value(), StandardCharsets.UTF_8) : null;
		}
	};

	@PostConstruct
	public void init() {
		for (java.lang.reflect.Method method : this.getClass().getDeclaredMethods()) {
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
		java.lang.reflect.Method handler = handlerMethods.get(event.getClass());
		if (nonNull(handler)) {
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
		log.debug("Received Kafka headers: {}", extractHeaders(record));
		log.debug("Processing Kafka message - topic: {}, partition: {}, offset: {}, key: {}",
				record.topic(), record.partition(), record.offset(), record.key());

		Context extractedContext = KafkaTracingPropagator.extract(Context.current(), record.headers());
		Tracer tracer = GlobalOpenTelemetry.get().getTracer("customer-read");
		Span consumerSpan = tracer.spanBuilder("Process Kafka message in customer-read")
				.setParent(extractedContext)
				.startSpan();
		try (Scope scope = consumerSpan.makeCurrent()) {
			try {
				Object value = record.value();
				if (!(value instanceof DomainEvent event)) {
					log.error("Received message is not a DomainEvent: {}", value);
					ack.acknowledge();
					return;
				}
				Map<String, String> headersMap = extractHeaders(record);
				boolean processed = processEvent(event, headersMap);
				if (!processed) {
					log.info("No handler found for event type: {}", event.getClass().getSimpleName());
				}
				ack.acknowledge();
			} catch (Exception e) {
				log.error("Error processing Kafka message: {}", e.getMessage(), e);
				ack.acknowledge();
			}
		} finally {
			consumerSpan.end();
		}
	}

	private Map<String, String> extractHeaders(ConsumerRecord<String, Object> record) {
		Map<String, String> result = new HashMap<>();
		for (Header header : record.headers()) {
			if (nonNull(header.key()) && nonNull(header.value())) {
				String headerValue = new String(header.value(), StandardCharsets.UTF_8);
				result.put(header.key(), headerValue);
			}
		}
		return result;
	}
}
