package pl.ecommerce.commons.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration
@Slf4j
public class ReactorHooksConfig {

	@PostConstruct
	public void initializeReactorHooks() {
		log.debug("Attempting to explicitly enable Reactor automatic context propagation hooks");
		try {
			Hooks.enableAutomaticContextPropagation();
			log.debug("Call to Hooks.enableAutomaticContextPropagation() completed.");
		} catch (Exception e) {
			log.error("Failed to enable Reactor automatic context propagation hooks via explicit call:", e);
		}
	}
}