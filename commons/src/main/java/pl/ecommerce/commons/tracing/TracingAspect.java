package pl.ecommerce.commons.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TracingAspect {

	private final ObservationRegistry observationRegistry;

	private static final TextMapGetter<HttpHeaders> HEADER_GETTER = new TextMapGetter<>() {
		@Override
		public Iterable<String> keys(HttpHeaders carrier) {
			return carrier.keySet();
		}

		@Override
		public String get(HttpHeaders carrier, String key) {
			return nonNull(carrier) ? carrier.getFirst(key) : null;
		}
	};

	@Around("@annotation(tracedOperation)")
	public Object traceOperation(ProceedingJoinPoint joinPoint, TracedOperation tracedOperation) throws Throwable {
		ServerWebExchange exchange = extractServerWebExchange(joinPoint.getArgs());
		if (isNull(exchange)) {
			log.warn("ServerWebExchange not found for operation: {}", tracedOperation.value());
			return joinPoint.proceed();
		}

		Context extractedContext = GlobalOpenTelemetry.getPropagators()
				.getTextMapPropagator()
				.extract(Context.current(), exchange.getRequest().getHeaders(), HEADER_GETTER);

		SpanBuilder spanBuilder = GlobalOpenTelemetry.getTracer("customer-write")
				.spanBuilder(tracedOperation.value())
				.setSpanKind(SpanKind.SERVER);

		if (Span.fromContext(extractedContext).getSpanContext().isValid()) {
			spanBuilder.setParent(extractedContext);
		}

		Span span = spanBuilder.startSpan();

		try (Scope scope = span.makeCurrent()) {
			TracingContext tracingContext = TracingContext.builder()
					.traceId(span.getSpanContext().getTraceId())
					.spanId(span.getSpanContext().getSpanId())
					.sourceService("customer-write")
					.sourceOperation(tracedOperation.value())
					.userId(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
					.build();

			String traceId = tracingContext.getTraceId();

			MDC.put("traceId", traceId);
			log.info("Executing operation: {} with traceId: {}", tracedOperation.value(), traceId);

			try {
				Observation observation = Observation.createNotStarted(tracedOperation.value(), observationRegistry)
						.lowCardinalityKeyValue("traceId", traceId);

				Object result = joinPoint.proceed();

				if (result instanceof Mono<?>) {
					return observation.observe(() -> ((Mono<?>) result)
							.contextWrite(ctx -> ctx.put(TracingContextHolder.CONTEXT_KEY, tracingContext))
							.doOnEach(signal -> {
								if (signal.hasValue() || signal.hasError()) {
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
							.doFinally(signalType -> MDC.remove("traceId")));
				}

				log.warn("Method must return Mono<ResponseEntity<?>>, but returned: {}",
						nonNull(result) ? result.getClass().getName() : "null");
				return result;
			} finally {
				MDC.remove("traceId");
			}
		} finally {
			span.end();
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
}