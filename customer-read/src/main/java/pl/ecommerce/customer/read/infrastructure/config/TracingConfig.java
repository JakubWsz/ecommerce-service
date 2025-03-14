//package pl.ecommerce.customer.read.infrastructure.config;
//
//import io.micrometer.observation.ObservationRegistry;
//import io.micrometer.observation.aop.ObservedAspect;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import io.micrometer.tracing.Tracer;
//import io.micrometer.tracing.propagation.Propagator;
//import org.springframework.web.server.WebFilter;
//import reactor.core.publisher.Hooks;
//import jakarta.annotation.PostConstruct;
//
///**
// * Configuration for distributed tracing in the read service
// */
//@Configuration
//public class TracingConfig {
//
//	@Bean
//	public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
//		return new ObservedAspect(observationRegistry);
//	}
//
//	/**
//	 * Initialize reactor hooks for context propagation
//	 */
//	@PostConstruct
//	public void init() {
//		Hooks.enableContextPropagation();
//	}
//
//	/**
//	 * WebFilter for propagating trace context in web requests
//	 */
//	@Bean
//	public WebFilter traceIdWebFilter(Tracer tracer, Propagator propagator) {
//		return (exchange, chain) -> {
//			var headers = exchange.getRequest().getHeaders();
//
//			String traceId = headers.getFirst("X-Trace-Id");
//			if (traceId == null) {
//				traceId = java.util.UUID.randomUUID().toString();
//			}
//
//			return chain.filter(exchange)
//					.doOnSuccessOrError((result, error) -> {
//						exchange.getResponse()
//								.getHeaders()
//								.add("X-Trace-Id", traceId);
//					});
//		};
//	}
//}