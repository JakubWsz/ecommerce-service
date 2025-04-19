package pl.ecommerce.customer.read.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ecommerce.commons.kafka.TopicsProvider;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

	private final KafkaProperties kafkaProperties;

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
//
//	@Bean
//	public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
//		return new ObservedAspect(observationRegistry);
//	}

//	@Bean
//	public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate,
//												ObjectMapper objectMapper) {
//		return new DlqConfig(
//				kafkaProperties,
//				objectMapper,
//				kafkaTemplate).deadLetterErrorHandler();
//	}
}