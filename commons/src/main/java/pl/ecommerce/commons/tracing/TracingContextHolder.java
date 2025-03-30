package pl.ecommerce.commons.tracing;

import java.util.Objects;

public class TracingContextHolder {
	private static final ThreadLocal<TracingContext> contextHolder = new ThreadLocal<>();

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
}