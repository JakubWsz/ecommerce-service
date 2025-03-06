package pl.ecommerce.vendor.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

	@Bean
	public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
		return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
				.circuitBreakerConfig(CircuitBreakerConfig.custom()
						.slidingWindowSize(10)
						.failureRateThreshold(50)
						.waitDurationInOpenState(Duration.ofSeconds(10))
						.permittedNumberOfCallsInHalfOpenState(5)
						.build())
				.timeLimiterConfig(TimeLimiterConfig.custom()
						.timeoutDuration(Duration.ofSeconds(5))
						.build())
				.build());
	}

	@Bean
	public Customizer<ReactiveResilience4JCircuitBreakerFactory> productServiceCustomizer() {
		return factory -> factory.configure(builder -> builder
				.circuitBreakerConfig(CircuitBreakerConfig.custom()
						.slidingWindowSize(10)
						.failureRateThreshold(50)
						.waitDurationInOpenState(Duration.ofSeconds(10))
						.permittedNumberOfCallsInHalfOpenState(5)
						.build())
				.timeLimiterConfig(TimeLimiterConfig.custom()
						.timeoutDuration(Duration.ofSeconds(3))
						.build()), "productService");
	}

	@Bean
	public Customizer<ReactiveResilience4JCircuitBreakerFactory> paymentGatewayCustomizer() {
		return factory -> factory.configure(builder -> builder
				.circuitBreakerConfig(CircuitBreakerConfig.custom()
						.slidingWindowSize(10)
						.failureRateThreshold(50)
						.waitDurationInOpenState(Duration.ofSeconds(15))
						.permittedNumberOfCallsInHalfOpenState(3)
						.build())
				.timeLimiterConfig(TimeLimiterConfig.custom()
						.timeoutDuration(Duration.ofSeconds(10))
						.build()), "paymentGateway");
	}
}