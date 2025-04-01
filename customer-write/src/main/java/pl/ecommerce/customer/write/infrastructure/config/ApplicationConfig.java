package pl.ecommerce.customer.write.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import pl.ecommerce.commons.kafka.TopicsProvider;
import pl.ecommerce.commons.kafka.dlq.DlqConfig;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

	private final KafkaProperties kafkaProperties;

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return mapper;
	}

	@Bean
	public TopicsProvider topicsProvider() {
		TopicsProvider provider = new TopicsProvider();
		provider.setTopics(List.of(
				"customer.registered.event",
				"customer.updated.event",
				"customer.email-changed.event",
				"customer.email-verified.event",
				"customer.phone-verified.event",
				"customer.address-added.event",
				"customer.address-updated.event",
				"customer.address-removed.event",
				"customer.preferences-updated.event",
				"customer.deactivated.event",
				"customer.reactivated.event",
				"customer.deleted.event"
		));
		return provider;
	}

	@Bean
	public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
		return new ObservedAspect(observationRegistry);
	}

	@Bean
	public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate,
												ObjectMapper objectMapper) {
		return new DlqConfig(
				kafkaProperties,
				objectMapper,
				kafkaTemplate).deadLetterErrorHandler();
	}
}