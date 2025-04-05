package pl.ecommerce.commons.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.nonNull;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	private static final TextMapSetter<ProducerRecord<String, String>> KAFKA_SETTER =
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
			log.warn("Event {} does not have @Message annotation and will not be sent",
					event.getClass().getSimpleName());
			return Mono.empty();
		}

		return Mono.fromCallable(() -> {
			try {
				Span span = GlobalOpenTelemetry.getTracer("kafka-producer")
						.spanBuilder("publish-" + event.getEventType())
						.setSpanKind(SpanKind.PRODUCER)
						.startSpan();

				try (Scope scope = span.makeCurrent()) {
					String traceId = span.getSpanContext().getTraceId();
					String spanId = span.getSpanContext().getSpanId();

					event.setTraceId(traceId);
					event.setSpanId(spanId);

					String eventJson = objectMapper.writeValueAsString(event);
					String eventType = event.getEventType();
					String topic = event.getClass().getAnnotation(Message.class).value();
					String className = event.getClass().getName();

					ProducerRecord<String, String> record = (nonNull(partition))
							? new ProducerRecord<>(topic, partition, key, eventJson)
							: new ProducerRecord<>(topic, key, eventJson);

					span.setAttribute("messaging.system", "kafka");
					span.setAttribute("messaging.destination", topic);
					span.setAttribute("messaging.destination_kind", "topic");

					GlobalOpenTelemetry.getPropagators()
							.getTextMapPropagator()
							.inject(Context.current(), record, KAFKA_SETTER);

					if (nonNull(className) && !className.isEmpty()) {
						record.headers().add(new RecordHeader("__TypeId__",
								className.getBytes(StandardCharsets.UTF_8)));
					}

					log.debug("Publishing event {} to topic {} with traceId {}",
							eventType, topic, traceId);

					CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> future =
							kafkaTemplate.send(record).toCompletableFuture();

					return future.handle((result, error) -> {
						if (nonNull(error)) {
							log.error("Failed to publish event {}: {}",
									eventType, error.getMessage(), error);
							span.recordException(error);
						} else {
							log.debug("Successfully published event {} with offset {}",
									eventType, result.getRecordMetadata().offset());
						}
						span.end();
						return null;
					});
				}
			} catch (JsonProcessingException e) {
				log.error("Error serializing event {}: {}",
						event.getEventType(), e.getMessage(), e);
				return CompletableFuture.failedFuture(e);
			} catch (Exception e) {
				log.error("Unexpected error occurred while publishing event {}: {}",
						event.getEventType(), e.getMessage(), e);
				return CompletableFuture.failedFuture(e);
			}
		}).flatMap(Mono::fromFuture).then();
	}
}