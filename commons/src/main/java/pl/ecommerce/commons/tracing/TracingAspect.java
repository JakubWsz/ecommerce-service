package pl.ecommerce.commons.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static pl.ecommerce.commons.tracing.TracingContextHolder.CONTEXT_KEY;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TracingAspect {

	private final ObservationRegistry observationRegistry;

	@Around("@annotation(tracedOperation)")
	public Object traceOperation(ProceedingJoinPoint joinPoint, TracedOperation tracedOperation) throws Throwable {
		ServerWebExchange exchange = extractServerWebExchange(joinPoint.getArgs());
		if (exchange == null) {
			log.warn("ServerWebExchange not found for operation: {}", tracedOperation.value());
			return joinPoint.proceed();
		}

		TracingContext tracingContext = createTracingContext(exchange, tracedOperation.value());
		String traceId = tracingContext.getTraceId();

		MDC.put("traceId", traceId);

		log.info("Executing operation: {} with traceId: {}", tracedOperation.value(), traceId);

		TracingContextHolder.setContext(tracingContext);

		try {
			Observation observation = Observation.createNotStarted(tracedOperation.value(), observationRegistry)
					.lowCardinalityKeyValue("traceId", traceId);

			Object result = joinPoint.proceed();

			if (result instanceof Mono<?>) {
				return observation.observe(() -> ((Mono<?>) result)
						.contextWrite(ctx -> ctx.put(CONTEXT_KEY, tracingContext))
						.doOnEach(signal -> {
							if (signal.hasValue() || signal.hasError()) {
								TracingContextHolder.setContext(tracingContext);
								MDC.put("traceId", traceId);
							}
						})
						.map(response -> {
							if (response instanceof ResponseEntity<?> original) {
								if (!original.getHeaders().containsKey("X-Trace-Id")) {
									HttpHeaders headers = new HttpHeaders();
									headers.putAll(original.getHeaders());
									headers.add("X-Trace-Id", traceId);

									return new ResponseEntity<>(original.getBody(), headers, original.getStatusCode());
								}
							}
							return response;
						})
						.doFinally(signalType -> {
							MDC.remove("traceId");
							TracingContextHolder.clearContext();
						}));
			}

			log.warn("Method must return Mono<ResponseEntity<?>>, but returned: {}",
					result != null ? result.getClass().getName() : "null");
			return result;
		} finally {
			MDC.remove("traceId");
			TracingContextHolder.clearContext();
		}
	}

	private ServerWebExchange extractServerWebExchange(Object[] args) {
		for (Object arg : args) {
			if (arg instanceof ServerWebExchange) {
				return (ServerWebExchange) arg;
			}
		}
		return null;
	}

	private TracingContext createTracingContext(ServerWebExchange exchange, String operation) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		String traceId = headers.getFirst("X-Trace-Id");
		if (traceId == null) {
			traceId = UUID.randomUUID().toString();
		}
		String userId = headers.getFirst("X-User-Id");

		return TracingContext.builder()
				.traceId(traceId)
				.spanId(UUID.randomUUID().toString())
				.userId(userId)
				.sourceService("customer-write")
				.sourceOperation(operation)
				.build();
	}
}