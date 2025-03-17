package pl.ecommerce.commons.tracing;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.util.HashMap;
import java.util.Map;

public class KafkaTraceContextExtractor {

	public static TracingContext extractTraceContext(ConsumerRecord<?, ?> record) {
		Map<String, String> headerMap = extractHeaders(record.headers());
		return TracingContext.fromHeadersMap(headerMap);
	}

	public static Map<String, String> extractHeaders(Headers headers) {
		Map<String, String> headerMap = new HashMap<>();

		for (Header header : headers) {
			String key = header.key();
			byte[] value = header.value();

			if (value != null) {
				headerMap.put(key, new String(value));
			}
		}

		return headerMap;
	}
}