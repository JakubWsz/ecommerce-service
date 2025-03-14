package pl.ecommerce.customer.write.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ecommerce.commons.kafka.TopicsProvider;

import java.util.List;

@Configuration
public class WriteAppConfig {
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
}
