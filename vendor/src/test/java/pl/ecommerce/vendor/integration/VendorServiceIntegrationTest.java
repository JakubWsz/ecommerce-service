package pl.ecommerce.vendor.integration;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.domain.model.Address;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.domain.service.VendorService;
import pl.ecommerce.vendor.infrastructure.exception.ValidationException;
import pl.ecommerce.vendor.infrastructure.exception.VendorAlreadyExistsException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@EmbeddedKafka(partitions = 1, topics = {
		"vendor.categories.assigned.event",
		"vendor.payment.processed.event",
		"vendor.registered.event",
		"vendor.status.changed.event",
		"vendor.updated.event",
		"vendor.verification.completed.event"
})
@ActiveProfiles("test")
public class VendorServiceIntegrationTest {

	@Container
	private static final MongoDBContainer MONGO_DB = new MongoDBContainer(
			DockerImageName.parse("mongo:6-focal"))
			.withExposedPorts(27017)
			.withStartupTimeout(Duration.ofSeconds(60))
			.waitingFor(Wait.forListeningPort());

	@Container
	private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
			DockerImageName.parse("apache/kafka-native:3.8.0"))
			.withStartupTimeout(Duration.ofMinutes(2))
			.withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
			.withEnv("KAFKA_NUM_PARTITIONS", "1")
			.withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
			.withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
			.withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
			.withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", "1")
			.withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", () -> String.format(
				"mongodb://%s:%d/vendor-service-test",
				MONGO_DB.getHost(), MONGO_DB.getFirstMappedPort()));
		registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
	}

	@Autowired
	private VendorRepository vendorRepository;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private VendorService vendorService;

	private static final UUID VENDOR_ID = UUID.randomUUID();
	private static final String VENDOR_EMAIL = "test.vendor@example.com";
	private Vendor testVendor;
	private static KafkaConsumer<String, String> consumer;

	@BeforeEach
	void setupBeforeEach() {
		vendorRepository.deleteAll().block();

		Address businessAddress = Address.builder()
				.street("123 Main St")
				.buildingNumber("1")
				.city("Test City")
				.state("Test State")
				.postalCode("12345")
				.country("Test Country")
				.build();

		testVendor = Vendor.builder()
				.id(VENDOR_ID)
				.email(VENDOR_EMAIL)
				.name("Test Vendor")
				.description("Test Description")
				.phone("123456789")
				.businessName("Test Business")
				.taxId("TAX-123456")
				.businessAddress(businessAddress)
				.bankAccountDetails("Test Bank Account")
				.vendorStatus(Vendor.VendorStatus.ACTIVE)
				.verificationStatus(Vendor.VerificationStatus.PENDING)
				.build();

		vendorRepository.save(testVendor).block();

		vendorService = new VendorService(vendorRepository, eventPublisher);

		consumer.poll(Duration.ofMillis(100));
	}

	@BeforeAll
	static void setupBeforeAll() {
		MONGO_DB.start();
		KAFKA_CONTAINER.start();

		setupKafkaConsumer();
		waitForKafkaReady();
	}

	@AfterAll
	static void afterAll() {
		MONGO_DB.stop();
		MONGO_DB.close();
		KAFKA_CONTAINER.stop();
		KAFKA_CONTAINER.close();
	}

	@Test
	void registerVendor_WithValidData_ShouldRegisterSuccessfully() {
		Vendor newVendor = Vendor.builder()
				.email("new.vendor@example.com")
				.name("New Vendor")
				.businessName("New Business")
				.gdprConsent(true)
				.build();

		Vendor vendor = vendorService.registerVendor(newVendor).block();

		assertThat(vendor).isNotNull();
		assertThat(vendor.getId()).isNotNull();
		assertThat(vendor.getEmail()).isEqualTo("new.vendor@example.com");
		assertThat(vendor.getName()).isEqualTo("New Vendor");
		assertTrue(vendor.getActive());
		assertThat(vendor.getVendorStatus()).isEqualTo(Vendor.VendorStatus.PENDING);

		ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
		boolean eventReceived = records.records("vendor.registered.event").iterator().hasNext();

		assertTrue(eventReceived, "Expected VendorRegisteredEvent to be published.");
	}

	@Test
	void registerVendor_WithoutGdprConsent_ShouldFail() {
		Vendor invalidVendor = Vendor.builder()
				.email("nogdpr@example.com")
				.name("No GDPR Vendor")
				.gdprConsent(false)
				.build();

		Exception exception = null;
		try {
			vendorService.registerVendor(invalidVendor).block();
		} catch (Exception e) {
			exception = e;
		}

		assertNotNull(exception);
		assertInstanceOf(ValidationException.class, exception);
		assertTrue(exception.getMessage().contains("GDPR consent is required"));
	}

	@Test
	void registerVendor_WithExistingEmail_ShouldFail() {
		vendorRepository.save(testVendor).block();

		Vendor duplicateVendor = Vendor.builder()
				.email(VENDOR_EMAIL)
				.name("Duplicate Vendor")
				.gdprConsent(true)
				.build();

		StepVerifier.create(vendorService.registerVendor(duplicateVendor))
				.expectErrorMatches(e -> e instanceof VendorAlreadyExistsException)
				.verify();

	}

	@Test
	void getVendorById_ExistingVendor_ShouldReturnVendor() {
		vendorRepository.save(testVendor).block();

		StepVerifier.create(vendorService.getVendorById(VENDOR_ID))
				.assertNext(vendor -> {
					assertThat(vendor).isNotNull();
					assertThat(vendor.getId()).isEqualTo(VENDOR_ID);
					assertThat(vendor.getEmail()).isEqualTo(VENDOR_EMAIL);
				})
				.verifyComplete();
	}

	@Test
	void getVendorById_NonExistingVendor_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		StepVerifier.create(vendorService.getVendorById(nonExistingId))
				.expectErrorMatches(throwable -> throwable instanceof VendorNotFoundException)
				.verify();
	}

	@Test
	void getAllVendors_ShouldReturnAllActiveVendors() {
		vendorRepository.save(testVendor).block();

		Vendor secondVendor = Vendor.builder()
				.email("second@example.com")
				.name("Second Vendor")
				.active(true)
				.build();
		vendorRepository.save(secondVendor).block();

		StepVerifier.create(vendorService.getAllVendors().collectList())
				.assertNext(vendors -> {
					assertThat(vendors).hasSize(2);
					assertThat(vendors.stream().map(Vendor::getEmail))
							.contains(VENDOR_EMAIL, "second@example.com");
				})
				.verifyComplete();
	}

	@Test
	void updateVendor_ExistingVendor_ShouldUpdateSuccessfully() {
		Vendor vendorUpdate = Vendor.builder()
				.name("Updated Name")
				.description("Updated Description")
				.phone("987654321")
				.build();

		StepVerifier.create(vendorService.updateVendor(VENDOR_ID, vendorUpdate))
				.assertNext(vendor -> {
					assertThat(vendor).isNotNull();
					assertThat(vendor.getId()).isEqualTo(VENDOR_ID);
					assertThat(vendor.getName()).isEqualTo("Updated Name");
					assertThat(vendor.getDescription()).isEqualTo("Updated Description");
					assertThat(vendor.getPhone()).isEqualTo("987654321");
					assertThat(vendor.getEmail()).isEqualTo(VENDOR_EMAIL);
				})
				.verifyComplete();

		ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
		boolean eventReceived = records.records("vendor.updated.event").iterator().hasNext();

		assertTrue(eventReceived, "Expected VendorUpdatedEvent to be published.");
	}

	@Test
	void updateVendor_WithBusinessAddress_ShouldUpdateSuccessfully() {
		vendorRepository.save(testVendor).block();

		Address newAddress = Address.builder()
				.street("New Street")
				.buildingNumber("5")
				.city("New City")
				.state("New State")
				.postalCode("54321")
				.country("New Country")
				.build();

		Vendor vendorUpdate = Vendor.builder()
				.businessAddress(newAddress)
				.build();

		StepVerifier.create(vendorService.updateVendor(VENDOR_ID, vendorUpdate))
				.assertNext(vendor -> {
					assertThat(vendor).isNotNull();
					assertThat(vendor.getId()).isEqualTo(VENDOR_ID);
					assertThat(vendor.getBusinessAddress()).isNotNull();
					assertThat(vendor.getBusinessAddress().getStreet()).isEqualTo("New Street");
					assertThat(vendor.getBusinessAddress().getCity()).isEqualTo("New City");
				})
				.verifyComplete();

		ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
		boolean eventReceived = records.records("vendor.updated.event").iterator().hasNext();

		assertTrue(eventReceived, "Expected VendorUpdatedEvent to be published.");
	}

	@Test
	void updateVendor_NonExistingVendor_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		Vendor vendorUpdate = Vendor.builder()
				.name("Updated Name")
				.build();

		StepVerifier.create(vendorService.updateVendor(nonExistingId, vendorUpdate))
				.expectErrorMatches(throwable -> throwable instanceof VendorNotFoundException)
				.verify();
	}

	@Test
	void updateVendorStatus_ExistingVendor_ShouldUpdateStatusSuccessfully() {
		vendorRepository.save(testVendor).block();

		StepVerifier.create(vendorService.updateVendorStatus(VENDOR_ID, Vendor.VendorStatus.SUSPENDED, "Violation"))
				.assertNext(vendor -> {
					assertThat(vendor).isNotNull();
					assertThat(vendor.getId()).isEqualTo(VENDOR_ID);
					assertThat(vendor.getVendorStatus()).isEqualTo(Vendor.VendorStatus.SUSPENDED);
					assertThat(vendor.getStatusChangeReason()).isEqualTo("Violation");
				})
				.verifyComplete();

		ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
		boolean eventReceived = records.records("vendor.status.changed.event").iterator().hasNext();

		assertTrue(eventReceived, "Expected VendorStatusChangedEvent to be published.");
	}

	@Test
	void updateVerificationStatus_ExistingVendor_ShouldUpdateVerificationSuccessfully() {
		vendorRepository.save(testVendor).block();

		StepVerifier.create(vendorService.updateVerificationStatus(VENDOR_ID, Vendor.VerificationStatus.VERIFIED))
				.assertNext(vendor -> {
					assertThat(vendor).isNotNull();
					assertThat(vendor.getId()).isEqualTo(VENDOR_ID);
					assertThat(vendor.getVerificationStatus()).isEqualTo(Vendor.VerificationStatus.VERIFIED);
				})
				.verifyComplete();

		ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
		boolean eventReceived = records.records("vendor.verification.completed.event").iterator().hasNext();

		assertTrue(eventReceived, "Expected VendorStatusChangedEvent to be published.");
	}

	@Test
	void deactivateVendor_ExistingVendor_ShouldDeactivateSuccessfully() {
		vendorRepository.save(testVendor).block();

		StepVerifier.create(vendorService.deactivateVendor(VENDOR_ID)
						.then(vendorRepository.findById(VENDOR_ID)))
				.assertNext(vendor -> {
					assertThat(vendor).isNotNull();
					assertThat(vendor.getId()).isEqualTo(VENDOR_ID);
					assertThat(vendor.getActive()).isFalse();
				})
				.verifyComplete();

		ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
		boolean eventReceived = records.records("vendor.status.changed.event").iterator().hasNext();

		assertTrue(eventReceived, "Expected VendorStatusChangedEvent to be published.");
	}

	@Test
	void deactivateVendor_NonExistingVendor_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		StepVerifier.create(vendorService.deactivateVendor(nonExistingId))
				.expectErrorMatches(throwable -> throwable instanceof VendorNotFoundException)
				.verify();
	}

	private static void setupKafkaConsumer() {
		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "10000");
		props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "3000");

		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(List.of(
				"vendor.categories.assigned.event",
				"vendor.payment.processed.event",
				"vendor.registered.event",
				"vendor.status.changed.event",
				"vendor.updated.event",
				"vendor.verification.completed.event"
		));

		try {
			consumer.poll(Duration.ofMillis(100));
		} catch (Exception e) {
		}
	}

	private static void waitForKafkaReady() {
		int retries = 20;
		int waitTimeMs = 5000;

		while (retries-- > 0) {
			try {
				if (KAFKA_CONTAINER.isRunning()) {
					try (AdminClient adminClient = createAdminClient()) {
						List<String> topics = List.of(
								"vendor.categories.assigned.event",
								"vendor.payment.processed.event",
								"vendor.registered.event",
								"vendor.status.changed.event",
								"vendor.updated.event",
								"vendor.verification.completed.event"
						);

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

	private static AdminClient createAdminClient() {
		Properties props = new Properties();
		props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
		return AdminClient.create(props);
	}
}