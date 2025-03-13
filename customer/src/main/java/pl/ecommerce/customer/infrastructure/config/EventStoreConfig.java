package pl.ecommerce.customer.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import pl.ecommerce.customer.infrastructure.eventstore.EventStore;
import pl.ecommerce.customer.infrastructure.eventstore.EventStoreHealthIndicator;
import pl.ecommerce.customer.infrastructure.eventstore.JdbcEventStore;
import pl.ecommerce.customer.infrastructure.outbox.OutboxRepository;
import pl.ecommerce.customer.infrastructure.outbox.JdbcOutboxRepository;
import pl.ecommerce.customer.infrastructure.outbox.OutboxProcessor;

import javax.sql.DataSource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Konfiguracja komponentów związanych z Event Store
 */
@Configuration
public class EventStoreConfig {

	@Value("${app.outbox.thread-pool-size:2}")
	private int outboxThreadPoolSize;

	@Value("${app.outbox.processing-interval-ms:500}")
	private long outboxProcessingIntervalMs;

	/**
	 * Konfiguracja Event Store
	 */
	@Bean
	public EventStore eventStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		// Upewnij się, że ObjectMapper ma zarejestrowany JavaTimeModule dla obsługi typów daty/czasu
		objectMapper.registerModule(new JavaTimeModule());
		return new JdbcEventStore(jdbcTemplate, objectMapper);
	}

	/**
	 * Repozytorium dla tabeli outbox
	 */
	@Bean
	public OutboxRepository outboxRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		return new JdbcOutboxRepository(jdbcTemplate, objectMapper);
	}

	/**
	 * Pula wątków dla procesora outbox
	 */
	@Bean
	public ScheduledExecutorService outboxExecutorService() {
		return Executors.newScheduledThreadPool(outboxThreadPoolSize);
	}

	/**
	 * Procesor cyklicznie przetwarzający zdarzenia z tabeli outbox
	 */
	@Bean
	public OutboxProcessor outboxProcessor(
			OutboxRepository outboxRepository,
			KafkaTemplate<String, Object> kafkaTemplate,
			ScheduledExecutorService executorService) {
		return new OutboxProcessor(outboxRepository, kafkaTemplate, executorService, outboxProcessingIntervalMs);
	}

	/**
	 * Health indicator dla Event Store
	 */
	@Bean
	public EventStoreHealthIndicator eventStoreHealthIndicator(DataSource dataSource) {
		return new EventStoreHealthIndicator(dataSource);
	}
}