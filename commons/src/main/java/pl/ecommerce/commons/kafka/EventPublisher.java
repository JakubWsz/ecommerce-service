package pl.ecommerce.commons.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.Message;


@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public void publish(Object event) {
		if (!event.getClass().isAnnotationPresent(Message.class)) {
			log.warn("⚠ Event {} does not have @Message annotation and will not be sent", event.getClass().getSimpleName());
			return;
		}

		try {
			String eventType = event.getClass().getSimpleName();
			String topic = event.getClass().getAnnotation(Message.class).value();

			String jsonWithEventType = objectMapper.writeValueAsString(event);

			kafkaTemplate.send(new ProducerRecord<>(topic, jsonWithEventType))
					.whenComplete((result, ex) -> {
						if (ex == null) {
							log.info("Sent event: {}", eventType);
						} else {
							log.error("Failed to send event: {} ", eventType, ex);
						}
					});

		} catch (Exception e) {
			log.error("Error serializing event {} before sending to Kafka", event.getClass().getSimpleName(), e);
		}
	}
}