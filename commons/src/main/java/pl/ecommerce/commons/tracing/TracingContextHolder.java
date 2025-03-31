package pl.ecommerce.commons.tracing;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Objects;

public class TracingContextHolder {
	private static final ThreadLocal<TracingContext> contextHolder = new ThreadLocal<>();
	public static final String CONTEXT_KEY = "TRACING_CONTEXT";

	public static void setContext(TracingContext context) {
		contextHolder.set(context);
	}

	public static TracingContext getContext() {
		return contextHolder.get();
	}

	public static void clearContext() {
		contextHolder.remove();
	}

	public static String getTraceId() {
		return Objects.nonNull(contextHolder.get()) ? contextHolder.get().getTraceId() : "unknown";
	}

	public static Context putInContext(Context context, TracingContext tracingContext) {
		return context.put(CONTEXT_KEY, tracingContext);
	}

	public static TracingContext getFromContext(ContextView contextView) {
		return contextView.getOrDefault(CONTEXT_KEY, null);
	}

	public static <T> Mono<T> withContext(Mono<T> mono, TracingContext tracingContext) {
		return mono.contextWrite(ctx -> ctx.put(CONTEXT_KEY, tracingContext));
	}

	public static Mono<TracingContext> fromContextMono() {
		return Mono.deferContextual(ctx ->
				Mono.justOrEmpty(ctx.getOrDefault(CONTEXT_KEY, null)));
	}

	public static Mono<String> getTraceIdFromContext() {
		return fromContextMono()
				.map(TracingContext::getTraceId)
				.defaultIfEmpty("unknown");
	}
}