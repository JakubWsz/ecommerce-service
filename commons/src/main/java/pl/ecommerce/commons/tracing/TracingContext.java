package pl.ecommerce.commons.tracing;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.nonNull;
import static java.util.Objects.isNull;

@Slf4j
@Data
@NoArgsConstructor
public class TracingContext {
	public static final String TRACE_ID_HEADER = "trace-id";
	public static final String SPAN_ID_HEADER = "span-id";
	public static final String USER_ID_HEADER = "user-id";
	public static final String SOURCE_SERVICE_HEADER = "source-service";
	public static final String SOURCE_OPERATION_HEADER = "source-operation";
	public static final String TIMESTAMP_HEADER = "timestamp";

	private String traceId;
	private String spanId;
	private Instant timestamp;
	private String userId;
	private String sourceService;
	private String sourceOperation;
	private Map<String, Object> additionalData = new ConcurrentHashMap<>();

	@Builder
	public TracingContext(String traceId, String spanId, Instant timestamp,
						  String userId, String sourceService, String sourceOperation) {
		Span currentSpan = Span.current();

		if (nonNull(currentSpan) && currentSpan.getSpanContext().isValid()) {
			this.traceId = currentSpan.getSpanContext().getTraceId();
			this.spanId = currentSpan.getSpanContext().getSpanId();
		} else {
			this.traceId = (nonNull(traceId) && traceId.length() == 32)
					? traceId
					: generateTraceId();

			this.spanId = (nonNull(spanId) && spanId.length() == 16)
					? spanId
					: generateSpanId();
		}

		this.timestamp = (nonNull(timestamp)) ? timestamp : Instant.now();
		this.userId = userId;
		this.sourceService = sourceService;
		this.sourceOperation = sourceOperation;
		this.additionalData = new ConcurrentHashMap<>();
	}

	public static String generateTraceId() {
		Span currentSpan = Span.current();
		if (nonNull(currentSpan) && currentSpan.getSpanContext().isValid()) {
			return currentSpan.getSpanContext().getTraceId();
		}

		return UUID.randomUUID().toString().replace("-", "") +
				UUID.randomUUID().toString().replace("-", "").substring(0, 16);
	}

	public static String generateSpanId() {
		Span currentSpan = Span.current();
		if (nonNull(currentSpan) && currentSpan.getSpanContext().isValid()) {
			return currentSpan.getSpanContext().getSpanId();
		}

		return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
	}

	public TracingContext createChildContext(String operation) {
		return TracingContext.builder()
				.traceId(this.traceId)
				.spanId(generateSpanId())
				.timestamp(Instant.now())
				.userId(this.userId)
				.sourceService(this.sourceService)
				.sourceOperation(operation)
				.build();
	}

	public TracingContext addData(String key, Object value) {
		this.additionalData.put(key, value);
		return this;
	}

	public static TracingContext createNew(String serviceId, String operation, String userId) {
		Span currentSpan = Span.current();
		if (nonNull(currentSpan) && currentSpan.getSpanContext().isValid()) {
			SpanContext spanContext = currentSpan.getSpanContext();
			return TracingContext.builder()
					.traceId(spanContext.getTraceId())
					.spanId(spanContext.getSpanId())
					.timestamp(Instant.now())
					.userId(userId)
					.sourceService(serviceId)
					.sourceOperation(operation)
					.build();
		}

		return TracingContext.builder()
				.traceId(generateTraceId())
				.spanId(generateSpanId())
				.timestamp(Instant.now())
				.userId(userId)
				.sourceService(serviceId)
				.sourceOperation(operation)
				.build();
	}

	public static TracingContext createNew() {
		return TracingContext.builder()
				.traceId(generateTraceId())
				.spanId(generateSpanId())
				.timestamp(Instant.now())
				.userId(null)
				.sourceService("unknown")
				.sourceOperation("unknown")
				.build();
	}

	public Map<String, String> toHeadersMap() {
		Map<String, String> headers = new HashMap<>();
		headers.put(TRACE_ID_HEADER, traceId);
		headers.put(SPAN_ID_HEADER, spanId);
		headers.put(TIMESTAMP_HEADER, timestamp.toString());

		if (nonNull(userId)) {
			headers.put(USER_ID_HEADER, userId);
		}
		if (nonNull(sourceService)) {
			headers.put(SOURCE_SERVICE_HEADER, sourceService);
		}
		if (nonNull(sourceOperation)) {
			headers.put(SOURCE_OPERATION_HEADER, sourceOperation);
		}

		return headers;
	}

	public static TracingContext fromHeadersMap(Map<String, String> headers) {
		if (isNull(headers) || headers.isEmpty()) {
			return createNew("unknown", "unknown", null);
		}

		String traceId = headers.get(TRACE_ID_HEADER);
		String spanId = headers.get(SPAN_ID_HEADER);
		String userId = headers.get(USER_ID_HEADER);
		String sourceService = headers.get(SOURCE_SERVICE_HEADER);
		String sourceOperation = headers.get(SOURCE_OPERATION_HEADER);

		Instant timestamp = null;
		String timestampStr = headers.get(TIMESTAMP_HEADER);
		if (nonNull(timestampStr)) {
			try {
				timestamp = Instant.parse(timestampStr);
			} catch (Exception e) {
				log.info(e.getMessage());
			}
		}

		return TracingContext.builder()
				.traceId(traceId)
				.spanId(spanId)
				.timestamp(timestamp)
				.userId(userId)
				.sourceService(sourceService)
				.sourceOperation(sourceOperation)
				.build();
	}
}