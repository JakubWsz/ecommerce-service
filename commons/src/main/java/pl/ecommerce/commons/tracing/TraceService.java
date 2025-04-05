package pl.ecommerce.commons.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.nonNull;

@Service
@Slf4j
public class TraceService {

	@Value("${spring.application.name:unknown-service}")
	private String serviceName;

	private static final TextMapGetter<HttpHeaders> HTTP_HEADERS_GETTER = new TextMapGetter<>() {
		@Override
		public Iterable<String> keys(HttpHeaders carrier) {
			return carrier.keySet();
		}

		@Override
		public String get(HttpHeaders carrier, String key) {
			return nonNull(carrier) ? carrier.getFirst(key) : null;
		}
	};

	private static final TextMapSetter<Map<String, String>> HEADERS_SETTER =
			(carrier, key, value) -> {
		if (nonNull(carrier) && nonNull(key) && nonNull(value)) {
			carrier.put(key, value);
		}
	};

	public <T> T withSpan(String spanName, SpanKind kind, Supplier<T> operation) {
		SpanBuilder spanBuilder = GlobalOpenTelemetry.getTracer(serviceName)
				.spanBuilder(spanName)
				.setSpanKind(kind);

		Span span = spanBuilder.startSpan();

		try (Scope scope = span.makeCurrent()) {
			MDC.put("traceId", span.getSpanContext().getTraceId());
			return operation.get();
		} catch (Exception e) {
			span.recordException(e);
			throw e;
		} finally {
			span.end();
			MDC.remove("traceId");
		}
	}

	public <T> T withSpan(Context parentContext, String spanName, SpanKind kind, Supplier<T> operation) {
		SpanBuilder spanBuilder = GlobalOpenTelemetry.getTracer(serviceName)
				.spanBuilder(spanName)
				.setSpanKind(kind);

		if (nonNull(parentContext)) {
			spanBuilder.setParent(parentContext);
		}

		Span span = spanBuilder.startSpan();

		try (Scope scope = span.makeCurrent()) {
			// Dodaj traceId do MDC dla logowania
			MDC.put("traceId", span.getSpanContext().getTraceId());
			return operation.get();
		} catch (Exception e) {
			span.recordException(e);
			throw e;
		} finally {
			span.end();
			MDC.remove("traceId");
		}
	}

	public void addAttributes(Map<String, String> attributes) {
		Span span = Span.current();

		if (nonNull(span) && span.getSpanContext().isValid()) {
			attributes.forEach((key, value) ->
					span.setAttribute(AttributeKey.stringKey(key), value));
		}
	}

	public void addAttribute(String key, String value) {
		Span span = Span.current();

		if (nonNull(span) && span.getSpanContext().isValid() && nonNull(key) && nonNull(value)) {
			span.setAttribute(key, value);
		}
	}

	public Context extractContextFromHttpHeaders(HttpHeaders headers) {
		if (nonNull(headers)) {
			return GlobalOpenTelemetry.getPropagators()
					.getTextMapPropagator()
					.extract(Context.current(), headers, HTTP_HEADERS_GETTER);
		}
		return Context.current();
	}

	public void injectTraceContext(Map<String, String> headers) {
		if (nonNull(headers)) {
			Context currentContext = Context.current();
			GlobalOpenTelemetry.getPropagators()
					.getTextMapPropagator()
					.inject(currentContext, headers, HEADERS_SETTER);
		}
	}

	public String getCurrentTraceId() {
		Span span = Span.current();

		if (nonNull(span) && span.getSpanContext().isValid()) {
			return span.getSpanContext().getTraceId();
		}
		return UUID.randomUUID().toString().replace("-", "");
	}

	public String getCurrentSpanId() {
		Span span = Span.current();

		if (nonNull(span) && span.getSpanContext().isValid()) {
			return span.getSpanContext().getSpanId();
		}

		return null;
	}

	public <T> T withSpanFromReactor(ContextView reactorContext, String spanName, Function<String, T> operation) {
		return withSpan(spanName, SpanKind.INTERNAL, () -> {
			String traceId = getCurrentTraceId();
			return operation.apply(traceId);
		});
	}
}