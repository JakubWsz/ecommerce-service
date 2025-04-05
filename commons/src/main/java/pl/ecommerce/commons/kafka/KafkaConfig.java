package pl.ecommerce.commons.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaConfig {

	private final ObjectMapper objectMapper;
	private final KafkaProperties kafkaProperties;

	@Bean
	public ConsumerFactory<String, Object> kafkaConsumerFactory() {
		Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class.getName());

		props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

		props.put(JsonDeserializer.TRUSTED_PACKAGES, "pl.ecommerce.commons.event");
		props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "pl.ecommerce.commons.event.AbstractDomainEvent");
		props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
		DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(props);

		props.put("header.propagation", "*");

		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	public ProducerFactory<String, String> kafkaProducerFactory() {
		Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());

		props.put("header.propagation", "*");
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

		return new DefaultKafkaProducerFactory<>(props);
	}

	@Bean
	public KafkaTemplate<String, String> kafkaTemplate() {
		var kafkaTemplate = new KafkaTemplate<>(kafkaProducerFactory());
		kafkaTemplate.setObservationEnabled(true);
		return kafkaTemplate;
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(kafkaConsumerFactory());

		StringJsonMessageConverter messageConverter = new StringJsonMessageConverter(objectMapper);
		factory.setRecordMessageConverter(messageConverter);

		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

		return factory;
	}
}