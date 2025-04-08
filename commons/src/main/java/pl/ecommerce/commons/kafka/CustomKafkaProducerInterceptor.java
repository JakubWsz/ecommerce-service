package pl.ecommerce.commons.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import io.opentelemetry.context.Context;
import pl.ecommerce.commons.tracing.KafkaTracingPropagator;

import java.util.Map;

@Slf4j
public class CustomKafkaProducerInterceptor implements ProducerInterceptor<String, String> {

	@Override
	public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
		if (record.headers().lastHeader("traceparent") == null) {
			KafkaTracingPropagator.inject(Context.current(), record.headers());
		}
		return record;
	}

	@Override
	public void onAcknowledgement(RecordMetadata metadata, Exception exception) {}

	@Override
	public void close() {}

	@Override
	public void configure(Map<String, ?> configs) {}
}

