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
import org.mockito.Mockito;
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
import pl.ecommerce.vendor.api.dto.CategoryAssignmentRequest;
import pl.ecommerce.vendor.domain.model.Address;
import pl.ecommerce.vendor.domain.model.Category;
import pl.ecommerce.vendor.domain.model.CategoryAssignment;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.CategoryAssignmentRepository;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.domain.service.CategoryService;
import pl.ecommerce.vendor.domain.service.VendorService;
import pl.ecommerce.vendor.infrastructure.client.ProductServiceClient;
import pl.ecommerce.vendor.infrastructure.exception.CategoryAssignmentException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@EmbeddedKafka(partitions = 1, topics = {
		"vendor.categories.assigned.event"
})
@ActiveProfiles("test")
public class CategoryServiceIntegrationTest {

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
		registry.add("spring.data.mongodb.dot-replacement", () -> "_");
		registry.add("spring.data.mongodb.field-naming-strategy",
				() -> "org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy");
		registry.add("spring.data.mongodb.auto-index-creation", () -> "true");

		registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
	}

	@Autowired
	private VendorRepository vendorRepository;

	@Autowired
	private CategoryAssignmentRepository categoryAssignmentRepository;

	@Autowired
	private EventPublisher eventPublisher;

	@Mock
	private ProductServiceClient productServiceClient;

	private VendorService vendorService;
	private CategoryService categoryService;

	private static final UUID VENDOR_ID = UUID.randomUUID();
	private static final UUID CATEGORY_ID_1 = UUID.randomUUID();
	private static final UUID CATEGORY_ID_2 = UUID.randomUUID();
	private static final String VENDOR_EMAIL = "test.vendor@example.com";
	private Vendor testVendor;
	private static KafkaConsumer<String, String> consumer;
	private List<ProductServiceClient.CategoryResponse> categoryResponses;
	private MonetaryAmount commissionRate;

	@BeforeEach
	void setupBeforeEach() {
		vendorRepository.deleteAll().block();
		categoryAssignmentRepository.deleteAll().block();

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
				.active(true)
				.build();

		vendorRepository.save(testVendor).block();

		vendorService = new VendorService(vendorRepository, eventPublisher);
		categoryService = new CategoryService(categoryAssignmentRepository, vendorService,
				productServiceClient, eventPublisher);

		commissionRate = Monetary.getDefaultAmountFactory()
				.setCurrency(Monetary.getCurrency("USD"))
				.setNumber(10.0).create();

		categoryResponses = List.of(
				new ProductServiceClient.CategoryResponse(
						CATEGORY_ID_1,
						"Electronics",
						"Electronic devices and accessories"),
				new ProductServiceClient.CategoryResponse(
						CATEGORY_ID_2,
						"Home & Garden",
						"Home improvement and garden supplies")
		);


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
	void assignCategories_WithValidData_ShouldAssignSuccessfully() {
		Mockito.reset(productServiceClient);

		when(productServiceClient.getCategories(Mockito.argThat(list ->
				list.size() == 1 && list.contains(CATEGORY_ID_1))))
				.thenReturn(Flux.just(categoryResponses.get(0)));

		// Prepare assignment requests
		List<CategoryAssignmentRequest> requests = List.of(
				new CategoryAssignmentRequest(CATEGORY_ID_1.toString(), commissionRate)
		);

		// Test category assignment
		StepVerifier.create(categoryService.assignCategories(VENDOR_ID, requests).collectList())
				.assertNext(assignments -> {
					assertThat(assignments).hasSize(1);
					assertThat(assignments.get(0).getVendorId()).isEqualTo(VENDOR_ID);
					assertThat(assignments.get(0).getCategory().getId()).isEqualTo(CATEGORY_ID_1);
					assertThat(assignments.get(0).getStatus()).isEqualTo(CategoryAssignment.CategoryAssignmentStatus.ACTIVE);
					assertThat(assignments.get(0).getCategoryCommissionRate()).isEqualTo(commissionRate);
				})
				.verifyComplete();

		// Verify Kafka event was published
		ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
		boolean eventReceived = records.records("vendor.categories.assigned.event").iterator().hasNext();

		assertTrue(eventReceived, "Expected VendorCategoriesAssignedEvent to be published.");
	}

	@Test
	void assignCategories_ForInactiveVendor_ShouldFail() {
		when(productServiceClient.getCategories(Mockito.anyList()))
				.thenReturn(Flux.fromIterable(categoryResponses));

		testVendor.setActive(false);
		vendorRepository.save(testVendor).block();

		List<CategoryAssignmentRequest> requests = List.of(
				new CategoryAssignmentRequest(CATEGORY_ID_1.toString(), commissionRate)
		);

		StepVerifier.create(categoryService.assignCategories(VENDOR_ID, requests))
				.expectError(CategoryAssignmentException.class)
				.verify();
	}

	@Test
	void assignCategories_ForNonExistingVendor_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		List<CategoryAssignmentRequest> requests = List.of(
				new CategoryAssignmentRequest(CATEGORY_ID_1.toString(), commissionRate)
		);

		StepVerifier.create(categoryService.assignCategories(nonExistingId, requests))
				.expectError(VendorNotFoundException.class)
				.verify();
	}

	@Test
	void assignCategories_WithAlreadyAssignedCategory_ShouldFail() {
		when(productServiceClient.getCategories(Mockito.anyList()))
				.thenReturn(Flux.fromIterable(categoryResponses));

		CategoryAssignment existingAssignment = CategoryAssignment.builder()
				.vendorId(VENDOR_ID)
				.category(Category.builder().id(CATEGORY_ID_1).name("Electronics").build())
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode())
				.build();

		categoryAssignmentRepository.save(existingAssignment).block();

		List<CategoryAssignmentRequest> requests = List.of(
				new CategoryAssignmentRequest(CATEGORY_ID_1.toString(), commissionRate)
		);

		StepVerifier.create(categoryService.assignCategories(VENDOR_ID, requests))
				.expectError(CategoryAssignmentException.class)
				.verify();
	}

	@Test
	void getVendorCategories_ShouldReturnAllCategories() {
		CategoryAssignment assignment1 = CategoryAssignment.builder()
				.vendorId(VENDOR_ID)
				.category(Category.builder().id(CATEGORY_ID_1).name("Electronics").build())
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode()).build();

		CategoryAssignment assignment2 = CategoryAssignment.builder()
				.vendorId(VENDOR_ID)
				.category(Category.builder().id(CATEGORY_ID_2).name("Home & Garden").build())
				.status(CategoryAssignment.CategoryAssignmentStatus.INACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode())
				.build();

		categoryAssignmentRepository.saveAll(List.of(assignment1, assignment2)).blockLast();

		StepVerifier.create(categoryService.getVendorCategories(VENDOR_ID).collectList())
				.assertNext(assignments -> {
					assertThat(assignments).hasSize(2);
					assertThat(assignments.stream().map(a -> a.getCategory().getId()))
							.containsExactlyInAnyOrder(CATEGORY_ID_1, CATEGORY_ID_2);
				})
				.verifyComplete();
	}

	@Test
	void getVendorActiveCategories_ShouldReturnOnlyActiveCategories() {
		CategoryAssignment assignment1 = CategoryAssignment.builder()
				.vendorId(VENDOR_ID)
				.category(Category.builder().id(CATEGORY_ID_1).name("Electronics").build())
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode()).build();

		CategoryAssignment assignment2 = CategoryAssignment.builder()
				.vendorId(VENDOR_ID)
				.category(Category.builder().id(CATEGORY_ID_2).name("Home & Garden").build())
				.status(CategoryAssignment.CategoryAssignmentStatus.INACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode())
				.build();

		categoryAssignmentRepository.saveAll(List.of(assignment1, assignment2)).blockLast();

		StepVerifier.create(categoryService.getVendorActiveCategories(VENDOR_ID).collectList())
				.assertNext(assignments -> {
					assertThat(assignments).hasSize(1);
					assertThat(assignments.getFirst().getCategory().getId()).isEqualTo(CATEGORY_ID_1);
					assertThat(assignments.getFirst().getStatus()).isEqualTo(CategoryAssignment.CategoryAssignmentStatus.ACTIVE);
				})
				.verifyComplete();
	}

	@Test
	void updateCategoryStatus_ShouldUpdateStatusSuccessfully() {
		CategoryAssignment assignment = CategoryAssignment.builder()
				.vendorId(VENDOR_ID)
				.category(Category.builder().id(CATEGORY_ID_1).name("Electronics").build())
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode())
				.build();

		categoryAssignmentRepository.save(assignment).block();

		StepVerifier.create(categoryService.updateCategoryStatus(
						VENDOR_ID, CATEGORY_ID_1, CategoryAssignment.CategoryAssignmentStatus.INACTIVE))
				.assertNext(updatedAssignment -> {
					assertThat(updatedAssignment).isNotNull();
					assertThat(updatedAssignment.getVendorId()).isEqualTo(VENDOR_ID);
					assertThat(updatedAssignment.getCategory().getId()).isEqualTo(CATEGORY_ID_1);
					assertThat(updatedAssignment.getStatus()).isEqualTo(CategoryAssignment.CategoryAssignmentStatus.INACTIVE);
				})
				.verifyComplete();
	}

	@Test
	void updateCategoryStatus_WithInvalidStatus_ShouldFail() {
		CategoryAssignment assignment = CategoryAssignment.builder()
				.vendorId(VENDOR_ID)
				.category(Category.builder().id(CATEGORY_ID_1).name("Electronics").build())
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode())
				.build();

		categoryAssignmentRepository.save(assignment).block();

		StepVerifier.create(categoryService.updateCategoryStatus(
						VENDOR_ID, CATEGORY_ID_1, null))
				.expectError(CategoryAssignmentException.class)
				.verify();
	}

	@Test
	void removeCategory_ShouldRemoveSuccessfully() {
		CategoryAssignment assignment = CategoryAssignment.builder()
				.vendorId(VENDOR_ID)
				.category(Category.builder().id(CATEGORY_ID_1).name("Electronics").build())
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode())
				.build();

		categoryAssignmentRepository.save(assignment).block();

		StepVerifier.create(categoryService.removeCategory(VENDOR_ID, CATEGORY_ID_1)
						.then(categoryAssignmentRepository.findByVendorIdAndCategoryId(VENDOR_ID, CATEGORY_ID_1)))
				.verifyComplete();
	}

	@Test
	void removeCategory_NonExistingAssignment_ShouldFail() {
		StepVerifier.create(categoryService.removeCategory(VENDOR_ID, UUID.randomUUID()))
				.expectError(CategoryAssignmentException.class)
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
		consumer.subscribe(List.of("vendor.categories.assigned.event"));

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
						List<String> topics = List.of("vendor.categories.assigned.event");

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
			} catch (InterruptedException ignored) {
			}
		}

		throw new IllegalStateException("Kafka is not ready after waiting.");
	}

	private static AdminClient createAdminClient() {
		Properties props = new Properties();
		props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
		return AdminClient.create(props);
	}
}