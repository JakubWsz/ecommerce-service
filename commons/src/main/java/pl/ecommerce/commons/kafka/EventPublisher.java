package pl.ecommerce.commons.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.commons.tracing.TracingContextHolder;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
	public static final String CONTEXT_KEY = "TRACING_CONTEXT";
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	private static final TextMapSetter<ProducerRecord<String, String>> SETTER =
			(carrier, key, value) -> {
				if (nonNull(carrier) && nonNull(key) && nonNull(value)) {
					carrier.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8)));
				}
			};

	public Mono<Void> publish(DomainEvent event) {
		return publish(event, null, null);
	}

	public Mono<Void> publish(DomainEvent event, String key) {
		return publish(event, null, key);
	}

	public Mono<Void> publish(DomainEvent event, Integer partition) {
		return publish(event, partition, null);
	}

	public Mono<Void> publish(DomainEvent event, Integer partition, String key) {
		if (!event.getClass().isAnnotationPresent(Message.class)) {
			log.warn("Event {} does not have @Message annotation and will not be sent", event.getClass().getSimpleName());
			return Mono.empty();
		}

		return Mono.deferContextual(contextView -> {
			try {
				TracingContext tracingContext = getTracingContext(contextView, event);

				if (nonNull(tracingContext) && isNull(event.getTracingContext())) {
					event.setTracingContext(tracingContext);
				}

				String eventJson = objectMapper.writeValueAsString(event);
				String eventType = event.getEventType();
				String topic = event.getClass().getAnnotation(Message.class).value();
				String className = event.getClass().getName();

				ProducerRecord<String, String> record =
						(partition != null) ? new ProducerRecord<>(topic, partition, key, eventJson)
								: new ProducerRecord<>(topic, key, eventJson);

				addTracingHeaders(record, event);

				injectOpenTelemetryContext(record, tracingContext);

				if (nonNull(className) && !className.isEmpty()) {
					record.headers().add(new RecordHeader("__TypeId__",
							className.getBytes(StandardCharsets.UTF_8)));
				}

				String traceId = getTraceId(event, contextView);
				log.debug("Publishing event {} to topic {} with traceId {}",
						eventType, topic, traceId);

				kafkaTemplate.setObservationEnabled(true);
				CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record).toCompletableFuture();

				return Mono.fromFuture(future)
						.doOnSuccess(result -> log.debug("Successfully published event {} with offset {}",
								eventType, result.getRecordMetadata().offset()))
						.doOnError(error -> log.error("Failed to publish event {}: {}",
								eventType, error.getLocalizedMessage(), error))
						.then();
			} catch (JsonProcessingException e) {
				log.error("Error serializing event {}: {}", event.getEventType(), e.getLocalizedMessage(), e);
				return Mono.error(e);
			} catch (Exception e) {
				log.error("Unexpected error occurred while publishing event {}: {}",
						event.getEventType(), e.getLocalizedMessage(), e);
				return Mono.error(e);
			}
		});
	}

	private void injectOpenTelemetryContext(ProducerRecord<String, String> record, TracingContext tracingContext) {
		if (nonNull(tracingContext)) {
			Context currentContext = Context.current();

			Span currentSpan = Span.current();
			if (nonNull(currentSpan) && currentSpan.getSpanContext().isValid()) {
				GlobalOpenTelemetry.getPropagators()
						.getTextMapPropagator()
						.inject(currentContext, record, SETTER);
			} else {
				SpanContext spanContext = SpanContext.createFromRemoteParent(
						tracingContext.getTraceId(),
						tracingContext.getSpanId(),
						io.opentelemetry.api.trace.TraceFlags.getSampled(),
						io.opentelemetry.api.trace.TraceState.getDefault()
				);

				Context syntheticContext = currentContext.with(io.opentelemetry.api.trace.Span.wrap(spanContext));
				GlobalOpenTelemetry.getPropagators()
						.getTextMapPropagator()
						.inject(syntheticContext, record, SETTER);
			}
		}
	}

	private void addTracingHeaders(ProducerRecord<String, String> record, DomainEvent event) {
		TracingContext tracingContext = event.getTracingContext();
		if (nonNull(tracingContext)) {
			String traceId = nonNull(tracingContext.getTraceId()) ?
					tracingContext.getTraceId() : "unknown";
			record.headers().add(new RecordHeader("trace-id",
					traceId.getBytes(StandardCharsets.UTF_8)));

			String spanId = nonNull(tracingContext.getSpanId()) ?
					tracingContext.getSpanId() : "unknown";
			record.headers().add(new RecordHeader("span-id",
					spanId.getBytes(StandardCharsets.UTF_8)));

			if (nonNull(tracingContext.getUserId())) {
				record.headers().add(new RecordHeader("user-id",
						tracingContext.getUserId().getBytes(StandardCharsets.UTF_8)));
			}

			record.headers().add(new RecordHeader("source-service",
					"customer-write".getBytes(StandardCharsets.UTF_8)));

			String sourceOperation = nonNull(tracingContext.getSourceOperation()) ?
					tracingContext.getSourceOperation() : event.getEventType();
			record.headers().add(new RecordHeader("source-operation",
					sourceOperation.getBytes(StandardCharsets.UTF_8)));
		}
	}

	private TracingContext getTracingContext(ContextView contextView, DomainEvent event) {
		if (nonNull(event.getTracingContext())) {
			return event.getTracingContext();
		}

		TracingContext fromReactor = contextView.getOrDefault(TracingContextHolder.CONTEXT_KEY, null);
		if (nonNull(fromReactor)) {
			return fromReactor;
		}

		return TracingContext.createNew("unknown", "unknown", null);
	}


	private String getTraceId(DomainEvent event, ContextView contextView) {
		if (nonNull(event.getTracingContext()) && nonNull(event.getTracingContext().getTraceId())) {
			return event.getTracingContext().getTraceId();
		}

		try {
			TracingContext fromReactor = contextView.getOrDefault(TracingContextHolder.CONTEXT_KEY, null);
			if (nonNull(fromReactor) && nonNull(fromReactor.getTraceId())) {
				return fromReactor.getTraceId();
			}
		} catch (Exception e) {
			log.debug("Error getting traceId from Reactor context", e);
		}

		return TracingContext.createNew("unknown", "unknown", null).getTraceId();
	}
}