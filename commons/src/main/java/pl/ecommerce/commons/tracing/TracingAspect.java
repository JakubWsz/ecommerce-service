package pl.ecommerce.commons.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TracingAspect {

	private final ObservationRegistry observationRegistry;
	private final TraceService traceService;

	public static final String TRACE_ID_CONTEXT_KEY = "TRACE_ID";

	@Around("@annotation(tracedOperation)")
	public Object traceOperation(ProceedingJoinPoint joinPoint, TracedOperation tracedOperation) throws Throwable {
		ServerWebExchange exchange = extractServerWebExchange(joinPoint.getArgs());
		if (isNull(exchange)) {
			log.warn("ServerWebExchange not found for operation: {}", tracedOperation.value());
			return joinPoint.proceed();
		}

		Context extractedContext = traceService.extractContextFromHttpHeaders(exchange.getRequest().getHeaders());

		return traceService.withSpan(
				extractedContext,
				tracedOperation.value(),
				SpanKind.SERVER,
				() -> {
					Span span = Span.current();
					String traceId = span.getSpanContext().getTraceId();

					String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
					if (nonNull(userId)) {
						span.setAttribute("user.id", userId);
					}

					span.setAttribute("http.url", exchange.getRequest().getURI().toString());

					try {
						Observation observation = Observation.createNotStarted(tracedOperation.value(), observationRegistry)
								.lowCardinalityKeyValue("traceId", traceId);

						Object result = joinPoint.proceed();

						if (result instanceof Mono<?>) {
							return observation.observe(() -> ((Mono<?>) result)
									.contextWrite(ctx -> ctx.put(TRACE_ID_CONTEXT_KEY, traceId))
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
									}));
						}

						log.warn("Method must return Mono<ResponseEntity<?>>, but returned: {}",
								nonNull(result) ? result.getClass().getName() : "null");
						return result;
					} catch (Throwable e) {
						span.recordException(e);
						try {
							throw e;
						} catch (Throwable ex) {
							throw new RuntimeException(ex);
						}
					}
				});
	}

	private ServerWebExchange extractServerWebExchange(Object[] args) {
		for (Object arg : args) {
			if (arg instanceof ServerWebExchange) {
				return (ServerWebExchange) arg;
			}
		}
		return null;
	}
}