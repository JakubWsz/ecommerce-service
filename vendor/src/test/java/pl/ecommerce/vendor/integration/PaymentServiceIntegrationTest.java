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
import org.mockito.Mock;
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
import pl.ecommerce.vendor.api.dto.PaymentRequest;
import pl.ecommerce.vendor.api.dto.PaymentResponse;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.model.VendorPayment;
import pl.ecommerce.vendor.domain.repository.VendorPaymentRepository;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.domain.service.PaymentService;
import pl.ecommerce.vendor.infrastructure.client.PaymentClient;
import pl.ecommerce.vendor.infrastructure.exception.PaymentProcessingException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@EmbeddedKafka(partitions = 1, topics = {
		"vendor.payment.processed.event"
})
@ActiveProfiles("test")
public class PaymentServiceIntegrationTest {

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
	private VendorPaymentRepository paymentRepository;

	@Autowired
	private EventPublisher eventPublisher;

	@Mock
	private PaymentClient paymentClient;

	private PaymentService paymentService;

	private static final UUID VENDOR_ID = UUID.randomUUID();
	private static final UUID PAYMENT_ID = UUID.randomUUID();
	private static final String PAYMENT_METHOD = "BANK_TRANSFER";
	private Vendor testVendor;
	private VendorPayment testPayment;
	private MonetaryAmount amount;
	private static KafkaConsumer<String, String> consumer;

	@BeforeEach
	void setupBeforeEach() {
		vendorRepository.deleteAll().block();
		paymentRepository.deleteAll().block();

		// Initialize monetary amount
		amount = Monetary.getDefaultAmountFactory()
				.setCurrency("USD")
				.setNumber(100.00)
				.create();

		testVendor = Vendor.builder()
				.id(VENDOR_ID)
				.email("test.vendor@example.com")
				.name("Test Vendor")
				.businessName("Test Business")
				.active(true)
				.vendorStatus(Vendor.VendorStatus.ACTIVE)
				.verificationStatus(Vendor.VerificationStatus.VERIFIED)
				.build();

		testPayment = VendorPayment.builder()
				.id(PAYMENT_ID)
				.vendorId(VENDOR_ID)
				.amount(amount.getNumber().numberValue(BigDecimal.class))
				.currency(amount.getCurrency().getCurrencyCode())
				.paymentMethod(PAYMENT_METHOD)
				.status(VendorPayment.VendorPaymentStatus.PENDING)
				.createdAt(LocalDateTime.now())
				.build();

		vendorRepository.save(testVendor).block();
		paymentRepository.save(testPayment).block();

		paymentService = new PaymentService(paymentRepository, vendorRepository, paymentClient, eventPublisher);

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
	void createPayment_WithValidData_ShouldCreatePaymentSuccessfully() {
		MonetaryAmount paymentAmount = Monetary.getDefaultAmountFactory()
				.setCurrency("USD")
				.setNumber(250.00)
				.create();

		StepVerifier.create(paymentService.createPayment(VENDOR_ID, paymentAmount, "CREDIT_CARD"))
				.assertNext(payment -> {
					assertThat(payment).isNotNull();
					assertThat(payment.getId()).isNotNull();
					assertThat(payment.getVendorId()).isEqualTo(VENDOR_ID);
					assertThat(payment.getAmount()).isEqualTo(paymentAmount);
					assertThat(payment.getPaymentMethod()).isEqualTo("CREDIT_CARD");
					assertThat(payment.getStatus()).isEqualTo(VendorPayment.VendorPaymentStatus.PENDING);
				})
				.verifyComplete();
	}

	@Test
	void createPayment_WithInactiveVendor_ShouldFail() {
		// Update vendor to inactive
		testVendor.setActive(false);
		vendorRepository.save(testVendor).block();

		StepVerifier.create(paymentService.createPayment(VENDOR_ID, amount, PAYMENT_METHOD))
				.expectErrorMatches(throwable -> throwable instanceof PaymentProcessingException &&
						throwable.getMessage().contains("Cannot process payment for inactive vendor"))
				.verify();
	}

	@Test
	void createPayment_WithNonExistingVendor_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		StepVerifier.create(paymentService.createPayment(nonExistingId, amount, PAYMENT_METHOD))
				.expectErrorMatches(throwable -> throwable instanceof VendorNotFoundException)
				.verify();
	}

	@Test
	void processPayment_WithSuccessfulResponse_ShouldProcessPaymentSuccessfully() {
		LocalDateTime now = LocalDateTime.now();
		UUID referenceId = UUID.randomUUID();
		PaymentResponse successResponse = PaymentResponse.builder()
				.id(PAYMENT_ID)
				.vendorId(VENDOR_ID)
				.amount(amount)
				.status(VendorPayment.VendorPaymentStatus.PROCESSED.toString())
				.paymentMethod(PAYMENT_METHOD)
				.referenceId(referenceId)
				.paymentDate(now)
				.createdAt(now.minusMinutes(5))
				.updatedAt(now)
				.build();

		when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(Mono.just(successResponse));

		StepVerifier.create(paymentService.processPayment(PAYMENT_ID))
				.assertNext(payment -> {
					assertThat(payment).isNotNull();
					assertThat(payment.getId()).isEqualTo(PAYMENT_ID);
					assertThat(payment.getStatus()).isEqualTo(VendorPayment.VendorPaymentStatus.PROCESSED);
					assertThat(payment.getReferenceId()).isEqualTo(successResponse.referenceId());
					assertThat(payment.getPaymentDate()).isEqualTo(successResponse.paymentDate());
				})
				.verifyComplete();

		ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
		boolean eventReceived = records.records("vendor.payment.processed.event").iterator().hasNext();

		assertTrue(eventReceived, "Expected VendorPaymentProcessedEvent to be published.");
	}

	@Test
	void processPayment_WithFailedResponse_ShouldMarkPaymentAsFailed() {
		// Mock payment client response
		PaymentResponse failedResponse = PaymentResponse.builder()
				.id(PAYMENT_ID)
				.vendorId(VENDOR_ID)
				.amount(amount)
				.status(VendorPayment.VendorPaymentStatus.FAILED.toString())
				.paymentMethod(PAYMENT_METHOD)
				.notes("Insufficient funds")
				.createdAt(LocalDateTime.now())
				.build();

		when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(Mono.just(failedResponse));

		StepVerifier.create(paymentService.processPayment(PAYMENT_ID))
				.assertNext(payment -> {
					assertThat(payment).isNotNull();
					assertThat(payment.getId()).isEqualTo(PAYMENT_ID);
					assertThat(payment.getStatus()).isEqualTo(VendorPayment.VendorPaymentStatus.FAILED);
					assertThat(payment.getNotes()).isEqualTo("Insufficient funds");
				})
				.verifyComplete();
	}

	@Test
	void processPayment_WithAlreadyProcessedPayment_ShouldFail() {
		testPayment.setStatus(VendorPayment.VendorPaymentStatus.PROCESSED);
		paymentRepository.save(testPayment).block();

		StepVerifier.create(paymentService.processPayment(PAYMENT_ID))
				.expectErrorMatches(throwable -> throwable instanceof PaymentProcessingException &&
						throwable.getMessage().contains("Payment already processed"))
				.verify();
	}

	@Test
	void processPayment_WithNonExistingPayment_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		StepVerifier.create(paymentService.processPayment(nonExistingId))
				.expectErrorMatches(throwable -> throwable instanceof PaymentProcessingException)
				.verify();
	}

	@Test
	void getPayment_ExistingPayment_ShouldReturnPayment() {
		StepVerifier.create(paymentService.getPayment(PAYMENT_ID))
				.assertNext(payment -> {
					assertThat(payment).isNotNull();
					assertThat(payment.getId()).isEqualTo(PAYMENT_ID);
					assertThat(payment.getVendorId()).isEqualTo(VENDOR_ID);
				})
				.verifyComplete();
	}

	@Test
	void getPayment_NonExistingPayment_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		StepVerifier.create(paymentService.getPayment(nonExistingId))
				.expectErrorMatches(throwable -> throwable instanceof PaymentProcessingException)
				.verify();
	}

	@Test
	void getPayments_ExistingVendor_ShouldReturnAllPayments() {
		// Create a second payment for same vendor
		VendorPayment secondPayment = VendorPayment.builder()
				.vendorId(VENDOR_ID)
				.amount(amount.getNumber().numberValue(BigDecimal.class))
				.currency(amount.getCurrency().getCurrencyCode())
				.paymentMethod(PAYMENT_METHOD)
				.status(VendorPayment.VendorPaymentStatus.PENDING)
				.createdAt(LocalDateTime.now())
				.build();

		paymentRepository.save(secondPayment).block();

		StepVerifier.create(paymentService.getPayments(VENDOR_ID).collectList())
				.assertNext(payments -> {
					assertThat(payments).hasSize(2);
					assertThat(payments.stream().map(VendorPayment::getVendorId))
							.containsOnly(VENDOR_ID);
				})
				.verifyComplete();
	}

	@Test
	void getPaymentsByStatus_ShouldReturnFilteredPayments() {
		// Create a processed payment
		VendorPayment processedPayment = VendorPayment.builder()
				.vendorId(VENDOR_ID)
				.amount(amount.getNumber().numberValue(BigDecimal.class))
				.currency(amount.getCurrency().getCurrencyCode())
				.paymentMethod(PAYMENT_METHOD)
				.status(VendorPayment.VendorPaymentStatus.PROCESSED)
				.createdAt(LocalDateTime.now())
				.paymentDate(LocalDateTime.now())
				.build();

		paymentRepository.save(processedPayment).block();

		// Test filter by PENDING status
		StepVerifier.create(paymentService.getPaymentsByStatus(VENDOR_ID, VendorPayment.VendorPaymentStatus.PENDING).collectList())
				.assertNext(payments -> {
					assertThat(payments).hasSize(1);
					assertThat(payments.get(0).getStatus()).isEqualTo(VendorPayment.VendorPaymentStatus.PENDING);
				})
				.verifyComplete();

		// Test filter by PROCESSED status
		StepVerifier.create(paymentService.getPaymentsByStatus(VENDOR_ID, VendorPayment.VendorPaymentStatus.PROCESSED).collectList())
				.assertNext(payments -> {
					assertThat(payments).hasSize(1);
					assertThat(payments.get(0).getStatus()).isEqualTo(VendorPayment.VendorPaymentStatus.PROCESSED);
				})
				.verifyComplete();
	}

	@Test
	void getPaymentsByDateRange_ShouldReturnFilteredPayments() {
		// Create payments with different dates
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime yesterday = now.minusDays(1);
		LocalDateTime tomorrow = now.plusDays(1);

		// Update test payment to have yesterday's payment date
		testPayment.setPaymentDate(yesterday);
		testPayment.setStatus(VendorPayment.VendorPaymentStatus.PROCESSED);
		paymentRepository.save(testPayment).block();

		// Create another payment with tomorrow's payment date
		VendorPayment futurePayment = VendorPayment.builder()
				.vendorId(VENDOR_ID)
				.amount(amount.getNumber().numberValue(BigDecimal.class))
				.currency(amount.getCurrency().getCurrencyCode())
				.paymentMethod(PAYMENT_METHOD)
				.status(VendorPayment.VendorPaymentStatus.PROCESSED)
				.createdAt(now)
				.paymentDate(tomorrow)
				.build();

		paymentRepository.save(futurePayment).block();

		// Test date range query - should only return tomorrow's payment
		StepVerifier.create(paymentService.getPaymentsByDateRange(VENDOR_ID, now, tomorrow.plusDays(1)).collectList())
				.assertNext(payments -> {
					assertThat(payments).hasSize(1);
					assertThat(payments.getFirst().getPaymentDate()).isAfter(now);
				})
				.verifyComplete();

		// Test full date range query - should return both payments
		StepVerifier.create(paymentService.getPaymentsByDateRange(VENDOR_ID, yesterday.minusDays(1), tomorrow.plusDays(1)).collectList())
				.assertNext(payments -> {
					assertThat(payments).hasSize(2);
				})
				.verifyComplete();
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
		consumer.subscribe(List.of("vendor.payment.processed.event"));

		try {
			consumer.poll(Duration.ofMillis(100));
		} catch (Exception e) {
			// Ignore initial connection issues
		}
	}

	private static void waitForKafkaReady() {
		int retries = 20;
		int waitTimeMs = 5000;

		while (retries-- > 0) {
			try {
				if (KAFKA_CONTAINER.isRunning()) {
					try (AdminClient adminClient = createAdminClient()) {
						List<String> topics = List.of("vendor.payment.processed.event");

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