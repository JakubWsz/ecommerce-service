package pl.ecommerce.commons.tracing;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class KafkaTracingPropagator {

	private static final TextMapPropagator PROPAGATOR =
			GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

	private static final TextMapSetter<Headers> KAFKA_SETTER = (carrier, key, value) -> {
		if (isNull(carrier) || isNull(value)) return;
		carrier.remove(key);
		carrier.add(key, value.getBytes(StandardCharsets.UTF_8));
	};

	private static final TextMapGetter<Headers> KAFKA_GETTER = new TextMapGetter<Headers>() {
		@Override
		public Iterable<String> keys(Headers carrier) {
			return (isNull(carrier))
					? java.util.Collections.emptyList()
					: StreamSupport.stream(carrier.spliterator(), false)
					.map(Header::key)
					.collect(Collectors.toList());
		}

		@Override
		public String get(Headers carrier, String key) {
			if (isNull(carrier)) return null;
			Header header = carrier.lastHeader(key);
			return nonNull(header)
					? new String(header.value(), StandardCharsets.UTF_8)
					: null;
		}
	};

	public static void inject(Context context, Headers kafkaHeaders) {
		PROPAGATOR.inject(context, kafkaHeaders, KAFKA_SETTER);
	}

	public static Context extract(Context baseContext, Headers kafkaHeaders) {
		return PROPAGATOR.extract(baseContext, kafkaHeaders, KAFKA_GETTER);
	}
}
