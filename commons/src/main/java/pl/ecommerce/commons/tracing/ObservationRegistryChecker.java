package pl.ecommerce.commons.tracing;

import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ObservationRegistryChecker {
	private final ObservationRegistry registry;

	public ObservationRegistryChecker(ObservationRegistry registry) {
		this.registry = registry;
	}

	@PostConstruct
	public void checkRegistry() {
		if (registry != null) {
			log.info(">>> ObservationRegistry bean successfully injected!");
		} else {
			log.error("!!! ObservationRegistry bean NOT found !!!");
		}
	}
}
