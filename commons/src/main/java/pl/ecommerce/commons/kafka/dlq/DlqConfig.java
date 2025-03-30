package pl.ecommerce.commons.kafka.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import pl.ecommerce.commons.kafka.ErrorHandlerUtils;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "kafka.dlq.enabled", havingValue = "true", matchIfMissing = true)
public class DlqConfig {

	private final KafkaProperties kafkaProperties;
	private final ObjectMapper objectMapper;
	private final KafkaTemplate<String, String> kafkaTemplate;

	@Bean
	public ConsumerFactory<String, String> deadLetterQueueConsumerFactory() {
		Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> deadLetterQueueListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, String> factory =
				new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(deadLetterQueueConsumerFactory());
		factory.setRecordMessageConverter(new StringJsonMessageConverter(objectMapper));
		factory.setCommonErrorHandler(deadLetterErrorHandler());
		return factory;
	}

	@Bean
	public CommonErrorHandler deadLetterErrorHandler() {
		return ErrorHandlerUtils.createDeadLetterErrorHandler(
				kafkaTemplate,
				5,
				1000L,
				2.0,
				60000L
		);
	}
}