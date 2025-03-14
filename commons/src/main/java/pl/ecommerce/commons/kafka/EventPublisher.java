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
import java.util.concurrent.CompletableFuture;

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

			ProducerRecord<String, String> record =
					(partition != null) ? new ProducerRecord<>(topic, partition, key, eventJson)
							: new ProducerRecord<>(topic, key, eventJson);

			addTracingHeaders(record, event);
			addCustomHeaders(record, headers);
			record.headers().add(new RecordHeader("event-type", eventType.getBytes(StandardCharsets.UTF_8)));

			log.debug("Publishing event {} to topic {} with traceId {}", event.getEventType(), topic, event.getTracingContext() != null ? event.getTracingContext().getTraceId() : "unknown");

			CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record).toCompletableFuture();

			return Mono.fromFuture(future)
					.doOnSuccess(result -> log.debug("Successfully published event {} with offset {}", event.getEventType(), result.getRecordMetadata().offset()))
					.doOnError(error -> log.error("Failed to publish event {}: {}", event.getEventType(), error.getLocalizedMessage(), error))
					.then();
		} catch (JsonProcessingException e) {
			log.error("Error serializing event {}: {}", event.getEventType(), e.getLocalizedMessage(), e);
			return Mono.error(e);
		} catch (Exception e) {
			log.error("Unexpected error occurred while publishing event {}: {}", event.getEventType(), e.getLocalizedMessage(), e);
			return Mono.error(e);
		}
	}

	private void addTracingHeaders(ProducerRecord<String, String> record, DomainEvent event) {
		TracingContext tracingContext = event.getTracingContext();
		if (tracingContext != null) {
			record.headers().add(new RecordHeader("trace-id", tracingContext.getTraceId().getBytes(StandardCharsets.UTF_8)));
			record.headers().add(new RecordHeader("span-id", tracingContext.getSpanId().getBytes(StandardCharsets.UTF_8)));
			if (tracingContext.getUserId() != null) {
				record.headers().add(new RecordHeader("user-id", tracingContext.getUserId().getBytes(StandardCharsets.UTF_8)));
			}
			record.headers().add(new RecordHeader("source-service", "customer-write".getBytes(StandardCharsets.UTF_8)));
			record.headers().add(new RecordHeader("source-operation", (tracingContext.getSourceOperation() != null ? tracingContext.getSourceOperation() : event.getEventType()).getBytes(StandardCharsets.UTF_8)));
		}
	}

	private void addCustomHeaders(ProducerRecord<String, String> record, Map<String, String> headers) {
		if (headers != null && !headers.isEmpty()) {
			headers.forEach((name, value) -> record.headers().add(new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8))));
		}
	}
}
