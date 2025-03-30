package pl.ecommerce.commons.kafka;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import pl.ecommerce.commons.kafka.dlq.DlqMetrics;

import java.util.function.BiConsumer;

import static java.util.Objects.nonNull;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorHandlerUtils {

	public static CommonErrorHandler createDeadLetterErrorHandler(
			KafkaTemplate<String, String> kafkaTemplate,
			int retryAttempts,
			long initialIntervalMs,
			double multiplier,
			long maxIntervalMs) {

		return createDeadLetterErrorHandler(kafkaTemplate, retryAttempts, initialIntervalMs,
				multiplier, maxIntervalMs, null, null);
	}

	public static CommonErrorHandler createDeadLetterErrorHandler(
			KafkaTemplate<String, String> kafkaTemplate,
			int retryAttempts,
			long initialIntervalMs,
			double multiplier,
			long maxIntervalMs,
			DlqMetrics dlqMetrics,
			BiConsumer<Object, Exception> additionalAction) {

		ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(retryAttempts);
		backOff.setInitialInterval(initialIntervalMs);
		backOff.setMultiplier(multiplier);
		backOff.setMaxInterval(maxIntervalMs);

		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
				(record, exception) -> {
					String deadLetterTopic = record.topic() + ".DLT";
					log.error("Sending message to dead letter topic {} due to exception: {}",
							deadLetterTopic, exception.getMessage());

					if (nonNull(dlqMetrics)) {
						dlqMetrics.recordDlqMessage(record.topic());
					}

					if (nonNull(additionalAction)) {
						additionalAction.accept(record, exception);
					}

					return new TopicPartition(deadLetterTopic, record.partition());
				});

		DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

		errorHandler.addNotRetryableExceptions(
				org.springframework.kafka.support.converter.ConversionException.class,
				com.fasterxml.jackson.core.JsonParseException.class,
				com.fasterxml.jackson.databind.JsonMappingException.class,
				com.fasterxml.jackson.databind.exc.InvalidFormatException.class,
				org.apache.kafka.common.errors.RecordTooLargeException.class
		);

		return errorHandler;
	}
}