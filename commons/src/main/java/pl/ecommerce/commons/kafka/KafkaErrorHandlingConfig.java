package pl.ecommerce.commons.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import pl.ecommerce.commons.kafka.dlq.DlqMetrics;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaErrorHandlingConfig {

	private final KafkaProperties kafkaProperties;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final DlqMetrics dlqMetrics;

	@Value("${spring.kafka.listener.concurrency:1}")
	private int concurrency;

	@Value("${kafka.dlq.retry-attempts:3}")
	private int retryAttempts;

	@Value("${kafka.dlq.initial-interval-ms:1000}")
	private long initialIntervalMs;

	@Value("${kafka.dlq.multiplier:2.0}")
	private double multiplier;

	@Value("${kafka.dlq.max-interval-ms:60000}")
	private long maxIntervalMs;

	@Bean
	public ConsumerFactory<Object, Object> errorHandlingConsumerFactory() {
		Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

		props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

		props.put(JsonDeserializer.TRUSTED_PACKAGES, "pl.ecommerce.commons.event");
		props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "pl.ecommerce.commons.event.AbstractDomainEvent");
		props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
		props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, false);

		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<Object, Object> errorHandlingKafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(errorHandlingConsumerFactory());
		factory.setConcurrency(concurrency);
		factory.setCommonErrorHandler(createDeadLetterErrorHandler());
		factory.setRecordMessageConverter(new org.springframework.kafka.support.converter.JsonMessageConverter());
		return factory;
	}

	private CommonErrorHandler createDeadLetterErrorHandler() {
		return ErrorHandlerUtils.createDeadLetterErrorHandler(
				kafkaTemplate,
				retryAttempts,
				initialIntervalMs,
				multiplier,
				maxIntervalMs,
				dlqMetrics,
				null
		);
	}
}