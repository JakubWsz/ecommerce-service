package pl.ecommerce.vendor.read.infrastructure.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import pl.ecommerce.commons.kafka.TopicsProvider;

import java.util.List;

@Configuration
@EnableWebFlux
public class ReadAppConfig {

	@Bean
	public ObservationRegistry observationRegistry() {
		return ObservationRegistry.create();
	}

	@Bean
	public TopicsProvider vendorTopicsProvider() {
		TopicsProvider provider = new TopicsProvider();
		provider.setTopics(List.of(
				"vendor.registered.event",
				"vendor.updated.event",
				"vendor.deleted.event",
				"vendor.status-changed.event",
				"vendor.bank-details-updated.event",
				"vendor.category-assigned.event",
				"vendor.category-removed.event",
				"vendor.payment.processed.event",
				"vendor.verification-completed.event"
		));
		return provider;
	}
}
