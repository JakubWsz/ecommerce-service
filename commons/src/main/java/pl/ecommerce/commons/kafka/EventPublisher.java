package pl.ecommerce.commons.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Component
@Slf4j
public class EventPublisher {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

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

		try {
			log.debug("Publishing event - type: {}, topic: {}",
					event.getEventType(),
					event.getClass().getAnnotation(Message.class).value());

			String eventJson = objectMapper.writeValueAsString(event);
			String topic = event.getClass().getAnnotation(Message.class).value();

			ProducerRecord<String, String> record =
					(partition != null)
							? new ProducerRecord<>(topic, partition, key, eventJson)
							: new ProducerRecord<>(topic, key, eventJson);

			log.debug("Publishing event {} to topic {}", event.getEventType(), topic);
			CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record).toCompletableFuture();

			return Mono.fromFuture(future)
					.doOnSuccess(result -> log.debug("Successfully published event {} with offset {}",
							event.getEventType(), result.getRecordMetadata().offset()))
					.doOnError(error -> log.error("Failed to publish event {}: {}",
							event.getEventType(), error.getLocalizedMessage(), error))
					.then();
		} catch (JsonProcessingException e) {
			log.error("Error serializing event {}: {}", event.getEventType(), e.getLocalizedMessage(), e);
			return Mono.error(e);
		} catch (Exception e) {
			log.error("Unexpected error while publishing event {}: {}",
					event.getEventType(), e.getLocalizedMessage(), e);
			return Mono.error(e);
		}
	}
}
