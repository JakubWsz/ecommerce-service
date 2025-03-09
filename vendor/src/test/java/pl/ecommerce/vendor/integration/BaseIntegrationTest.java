package pl.ecommerce.vendor.integration;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

	@Container
	protected static final MongoDBContainer MONGO_DB = new MongoDBContainer(
			DockerImageName.parse("mongo:6-focal"))
			.withExposedPorts(27017)
			.withStartupTimeout(Duration.ofSeconds(60))
			.waitingFor(Wait.forListeningPort());

	@Container
	protected static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
			DockerImageName.parse("apache/kafka-native:3.8.0"))
			.withStartupTimeout(Duration.ofMinutes(2))
			.withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
			.withEnv("KAFKA_NUM_PARTITIONS", "1")
			.withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
			.withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
			.withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
			.withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", "1")
			.withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");

	protected static KafkaConsumer<String, String> kafkaConsumer;


	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", () -> String.format(
				"mongodb://%s:%d/vendor-service-test",
				MONGO_DB.getHost(), MONGO_DB.getFirstMappedPort()));
		registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);

//		 registry.add("spring.data.mongodb.dot-replacement", () -> "_");
//		 registry.add("spring.data.mongodb.field-naming-strategy",
//		        () -> "org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy");
//		 registry.add("spring.data.mongodb.auto-index-creation", () -> "true");
	}

	@BeforeAll
	static void setupTestEnvironment() {
		if (!MONGO_DB.isRunning()) {
			MONGO_DB.start();
		}
		if (!KAFKA_CONTAINER.isRunning()) {
			KAFKA_CONTAINER.start();
		}
	}

	/**
	 * Czyści środowisko testowe po wszystkich testach
	 */
	@AfterAll
	static void cleanupTestEnvironment() {
		if (kafkaConsumer != null) {
			kafkaConsumer.close();
		}
	}


	protected static void setupKafkaConsumer(List<String> topics) {
		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "10000");
		props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "3000");

		kafkaConsumer = new KafkaConsumer<>(props);
		kafkaConsumer.subscribe(topics);

		try {
			kafkaConsumer.poll(Duration.ofMillis(100));
		} catch (Exception e) {
		}
	}


	protected static void waitForKafkaReady(List<String> topics) {
		int retries = 20;
		int waitTimeMs = 5000;

		while (retries-- > 0) {
			try {
				if (KAFKA_CONTAINER.isRunning()) {
					try (AdminClient adminClient = createAdminClient()) {
						List<NewTopic> newTopics = topics.stream()
								.map(topic -> new NewTopic(topic, 1, (short) 1))
								.collect(Collectors.toList());

						try {
							adminClient.createTopics(newTopics);
							Thread.sleep(1000);

							Set<String> existingTopics = adminClient.listTopics().names().get();
							if (existingTopics.containsAll(topics)) {
								return;
							}
						} catch (Exception e) {
							if (e instanceof TopicExistsException) {
								return;
							}
							System.out.println("Error creating topics: " + e.getMessage());
						}
					}
				}
			} catch (Exception e) {
				System.out.println("Waiting for Kafka to be ready. Retries left: " + retries);
			}

			try {
				Thread.sleep(waitTimeMs);
			} catch (InterruptedException ignored) {}
		}

		throw new IllegalStateException("Kafka is not ready after waiting.");
	}

	protected static AdminClient createAdminClient() {
		Properties props = new Properties();
		props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
		return AdminClient.create(props);
	}

	@BeforeEach
	protected void prepareKafkaConsumer() {
		if (kafkaConsumer != null) {
			kafkaConsumer.poll(Duration.ofMillis(100));
		}
	}
}