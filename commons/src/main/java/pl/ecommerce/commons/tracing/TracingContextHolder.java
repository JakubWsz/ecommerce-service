package pl.ecommerce.commons.tracing;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

public class TracingContextHolder {
	public static final String CONTEXT_KEY = "TRACING_CONTEXT";

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
				Mono.justOrEmpty(ctx.<TracingContext>getOrDefault(CONTEXT_KEY, null))
		);
	}

	public static Mono<String> getTraceIdFromContext() {
		return fromContextMono().map(TracingContext::getTraceId).defaultIfEmpty("unknown");
	}
}
