package pl.ecommerce.customer.write.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ecommerce.commons.kafka.TopicsProvider;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

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
}