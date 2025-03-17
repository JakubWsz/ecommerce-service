package pl.ecommerce.commons.tracing;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
		this.traceId = traceId != null ? traceId : UUID.randomUUID().toString();
		this.spanId = spanId != null ? spanId : UUID.randomUUID().toString();
		this.timestamp = timestamp != null ? timestamp : Instant.now();
		this.userId = userId;
		this.sourceService = sourceService;
		this.sourceOperation = sourceOperation;
		this.additionalData = new ConcurrentHashMap<>();
	}

	public TracingContext createChildContext(String operation) {
		return TracingContext.builder()
				.traceId(this.traceId)
				.spanId(UUID.randomUUID().toString())
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
		return TracingContext.builder()
				.traceId(UUID.randomUUID().toString())
				.spanId(UUID.randomUUID().toString())
				.timestamp(Instant.now())
				.userId(userId)
				.sourceService(serviceId)
				.sourceOperation(operation)
				.build();
	}

	public Map<String, String> toHeadersMap() {
		Map<String, String> headers = new HashMap<>();
		headers.put(TRACE_ID_HEADER, traceId);
		headers.put(SPAN_ID_HEADER, spanId);
		headers.put(TIMESTAMP_HEADER, timestamp.toString());

		if (userId != null) {
			headers.put(USER_ID_HEADER, userId);
		}
		if (sourceService != null) {
			headers.put(SOURCE_SERVICE_HEADER, sourceService);
		}
		if (sourceOperation != null) {
			headers.put(SOURCE_OPERATION_HEADER, sourceOperation);
		}

		return headers;
	}

	public static TracingContext fromHeadersMap(Map<String, String> headers) {
		if (headers == null || headers.isEmpty()) {
			return createNew("unknown", "unknown", null);
		}

		String traceId = headers.get(TRACE_ID_HEADER);
		String spanId = headers.get(SPAN_ID_HEADER);
		String userId = headers.get(USER_ID_HEADER);
		String sourceService = headers.get(SOURCE_SERVICE_HEADER);
		String sourceOperation = headers.get(SOURCE_OPERATION_HEADER);

		Instant timestamp = null;
		String timestampStr = headers.get(TIMESTAMP_HEADER);
		if (timestampStr != null) {
			try {
				timestamp = Instant.parse(timestampStr);
			} catch (Exception e) {
				// If parsing fails, we'll use current time
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