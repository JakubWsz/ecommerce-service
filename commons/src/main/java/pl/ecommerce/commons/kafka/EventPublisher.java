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
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.nonNull;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public Mono<Void> publish(DomainEvent event) {
		return publish(event, null, null, null);
	}

	public Mono<Void> publish(DomainEvent event, String key) {
		return publish(event, null, key, null);
	}

	public Mono<Void> publish(DomainEvent event, Integer partition) {
		return publish(event, partition, null, null);
	}

	public Mono<Void> publish(DomainEvent event, String key, Map<String, String> headers) {
		return publish(event, null, key, headers);
	}

	public Mono<Void> publish(DomainEvent event, Integer partition, String key) {
		return publish(event, partition, key, null);
	}

	public Mono<Void> publish(DomainEvent event, Integer partition, String key, Map<String, String> headers) {
		if (!event.getClass().isAnnotationPresent(Message.class)) {
			log.warn("Event {} does not have @Message annotation and will not be sent", event.getClass().getSimpleName());
			return Mono.empty();
		}

		try {
			String eventJson = objectMapper.writeValueAsString(event);
			String eventType = event.getEventType();
			String topic = event.getClass().getAnnotation(Message.class).value();
			String className = event.getClass().getName();

			ProducerRecord<String, String> record =
					(partition != null) ? new ProducerRecord<>(topic, partition, key, eventJson)
							: new ProducerRecord<>(topic, key, eventJson);

			addTracingHeaders(record, event);

			addCustomHeaders(record, headers);

			if (nonNull(className) && !className.isEmpty()) {
				record.headers().add(new RecordHeader("__TypeId__",
						className.getBytes(StandardCharsets.UTF_8)));
			}

			String traceId = nonNull(event.getTracingContext()) ?
					event.getTracingContext().getTraceId() : "unknown";
			record.headers().add(new RecordHeader("trace-id",
					traceId.getBytes(StandardCharsets.UTF_8)));

			log.debug("Publishing event {} to topic {} with traceId {}",
					eventType, topic, traceId);

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

	private void addCustomHeaders(ProducerRecord<String, String> record, Map<String, String> headers) {
		if (nonNull(headers) && !headers.isEmpty()) {
			headers.forEach((name, value) -> {
				if (nonNull(value)) {
					record.headers().add(new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8)));
				} else {
					record.headers().add(new RecordHeader(name, new byte[0]));
				}
			});
		}
	}
}