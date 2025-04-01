package pl.ecommerce.commons.kafka.dlq;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class DlqMetrics {

	private final Counter dlqMessagesTotal;
	private final Counter dlqRetriesTotal;
	private final Counter dlqSuccessesTotal;
	private final Counter dlqFailuresTotal;
	private final Map<String, Counter> dlqTopicCounters = new ConcurrentHashMap<>();
	private final AtomicInteger dlqPendingGauge;
	private final Timer dlqProcessingTimer;
	private final String serviceName;

	public DlqMetrics(MeterRegistry registry,
					  @Value("${spring.application.name:unknown}") String serviceName) {
		this.serviceName=serviceName;

		dlqMessagesTotal = Counter.builder("dlq_messages_total")
				.description("Total number of messages sent to DLQ")
				.tag("service", serviceName)
				.register(registry);

		dlqRetriesTotal = Counter.builder("dlq_retries_total")
				.description("Total number of retry attempts")
				.tag("service", serviceName)
				.register(registry);

		dlqSuccessesTotal = Counter.builder("dlq_successes_total")
				.description("Total number of successful retries")
				.tag("service", serviceName)
				.register(registry);

		dlqFailuresTotal = Counter.builder("dlq_failures_total")
				.description("Total number of permanently failed messages")
				.tag("service", serviceName)
				.register(registry);

		dlqPendingGauge = registry.gauge("dlq_pending_count",
				new AtomicInteger(0));

		dlqProcessingTimer = Timer.builder("dlq_processing_seconds")
				.description("Time taken to process DLQ messages")
				.tag("service", serviceName)
				.register(registry);
	}

	public void recordDlqMessage(String topic) {
		dlqMessagesTotal.increment();
		getOrCreateTopicCounter(topic).increment();
	}

	public void recordRetryAttempt() {
		dlqRetriesTotal.increment();
	}

	public void recordRetrySuccess() {
		dlqSuccessesTotal.increment();
	}

	public void recordPermanentFailure() {
		dlqFailuresTotal.increment();
	}

	public void updatePendingCount(int count) {
		if (dlqPendingGauge != null) {
			dlqPendingGauge.set(count);
		}
	}

	public Timer.Sample startTimer() {
		return Timer.start();
	}

	public void stopTimer(Timer.Sample sample) {
		sample.stop(dlqProcessingTimer);
	}

	private Counter getOrCreateTopicCounter(String topic) {
		return dlqTopicCounters.computeIfAbsent(topic, t -> Counter.builder("dlq_messages_total")
				.description("Total number of messages sent to DLQ")
				.tag("service", serviceName)
				.tag("topic", t)
				.register(null));
	}
}