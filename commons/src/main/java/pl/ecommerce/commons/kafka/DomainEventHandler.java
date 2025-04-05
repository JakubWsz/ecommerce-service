package pl.ecommerce.commons.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.kafka.dlq.DlqMetrics;
import pl.ecommerce.commons.tracing.TraceService;

import java.lang.reflect.Method;
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

	private final TraceService traceService;

	@Autowired(required = false)
	private DlqMetrics dlqMetrics;

	private final Map<Class<? extends DomainEvent>, Method> handlerMethods = new HashMap<>();

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

	@KafkaListener(
			topics = "#{@topicsProvider.getTopics()}",
			groupId = "${event.listener.group-id:${spring.application.name}-group}",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
		TextMapGetter<Headers> headersGetter = new TextMapGetter<>() {
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

		Context extractedContext = GlobalOpenTelemetry.getPropagators()
				.getTextMapPropagator()
				.extract(Context.current(), record.headers(), headersGetter);

		Span span = GlobalOpenTelemetry.getTracer(applicationName)
				.spanBuilder("processKafkaEvent")
				.setSpanKind(SpanKind.CONSUMER)
				.setParent(extractedContext)
				.startSpan();

		String traceId = span.getSpanContext().getTraceId();

		try (Scope scope = span.makeCurrent()) {
			MDC.put("traceId", traceId);

			span.setAttribute("messaging.system", "kafka");
			span.setAttribute("messaging.destination", record.topic());
			span.setAttribute("messaging.operation", "process");

			Object value = record.value();

			if (!(value instanceof DomainEvent event)) {
				log.error("Received message is not a DomainEvent: {}, traceId: {}", value, traceId);
				ack.acknowledge();
				return;
			}

			span.setAttribute("messaging.event_type", event.getEventType());
			span.setAttribute("messaging.aggregate_id", event.getAggregateId().toString());

			Map<String, String> headers = extractHeaders(record);
			boolean processed = processEvent(event, headers);

			if (!processed) {
				log.info("No handler found for event type: {}, traceId: {}",
						event.getClass().getSimpleName(), traceId);
			}

			ack.acknowledge();
		} catch (Exception e) {
			log.error("Error processing Kafka message: {}", e.getMessage(), e);
			span.recordException(e);
			ack.acknowledge();
		} finally {
			span.end();
			MDC.remove("traceId");
		}
	}

	public boolean processEvent(DomainEvent event, Map<String, String> headers) {
		Method handler = handlerMethods.get(event.getClass());
		if (nonNull(handler)) {
			Span span = GlobalOpenTelemetry.getTracer(applicationName)
					.spanBuilder("handle-" + event.getEventType())
					.setSpanKind(SpanKind.INTERNAL)
					.startSpan();

			try (Scope scope = span.makeCurrent()) {
				span.setAttribute("event.type", event.getEventType());
				span.setAttribute("event.aggregate_id", event.getAggregateId().toString());
				span.setAttribute("event.version", event.getVersion());

				MDC.put("traceId", span.getSpanContext().getTraceId());

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
					span.recordException(e);
					return false;
				}
			} finally {
				span.end();
				MDC.remove("traceId");
			}
		}
		return false;
	}

	private Map<String, String> extractHeaders(ConsumerRecord<?, ?> record) {
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