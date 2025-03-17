package pl.ecommerce.vendor.write.infrastructure.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;

@Configuration
@EnableWebFlux
public class WriteAppConfig {

	@Bean
	public ObservationRegistry observationRegistry() {
		return ObservationRegistry.create();
	}
}