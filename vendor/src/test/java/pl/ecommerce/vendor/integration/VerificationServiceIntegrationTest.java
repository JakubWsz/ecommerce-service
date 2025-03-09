package pl.ecommerce.vendor.integration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.domain.model.VerificationDocument;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.VerificationDocumentRepository;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.domain.service.VerificationService;
import pl.ecommerce.vendor.domain.service.VendorService;
import pl.ecommerce.vendor.infrastructure.exception.DocumentNotFoundException;
import pl.ecommerce.vendor.infrastructure.exception.ValidationException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@EmbeddedKafka(partitions = 1, topics = {"vendor.verification.completed.event"})
@ActiveProfiles("test")
public class VerificationServiceIntegrationTest {

	@Container
	private static final MongoDBContainer MONGO_DB = new MongoDBContainer(DockerImageName.parse("mongo:6-focal"))
			.withExposedPorts(27017)
			.withStartupTimeout(Duration.ofSeconds(60))
			.waitingFor(Wait.forListeningPort());

	@Container
	private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer("apache/kafka-native:3.8.0");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", () -> String.format(
				"mongodb://%s:%d/vendor-service-test",
				MONGO_DB.getHost(), MONGO_DB.getFirstMappedPort()));
		registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);

	}

	@Autowired
	private VerificationDocumentRepository documentRepository;

	@Autowired
	private VendorRepository vendorRepository;

	@Autowired
	private VendorService vendorService;

	@Autowired
	private EventPublisher eventPublisher;

	private VerificationService verificationService;

	private static final UUID VENDOR_ID = UUID.randomUUID();
	private static final UUID DOCUMENT_ID = UUID.randomUUID();
	private static final String DOCUMENT_URL = "http://example.com/document.pdf";
	private Vendor testVendor;
	private VerificationDocument testDocument;
	private KafkaConsumer<String, String> consumer;

	@BeforeEach
	void setupBeforeEach() {
		MockitoAnnotations.openMocks(this);

		vendorRepository.deleteAll().block();
		documentRepository.deleteAll().block();

		testVendor = Vendor.builder()
				.id(VENDOR_ID)
				.email("vendor@example.com")
				.name("Test Vendor")
				.build();

		testDocument = VerificationDocument.builder()
				.id(DOCUMENT_ID)
				.vendorId(VENDOR_ID)
				.documentType(VerificationDocument.DocumentType.TAX_CERTIFICATE)
				.documentUrl(DOCUMENT_URL)
				.status(VerificationDocument.VerificationStatus.PENDING)
				.build();

		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singletonList("vendor.verification.completed.event"));

		vendorRepository.save(testVendor).block();
		documentRepository.save(testDocument).block();

		verificationService = new VerificationService(documentRepository, vendorService);
	}

	@BeforeAll
	static void setupBeforeAll() {
		MONGO_DB.start();
		KAFKA_CONTAINER.start();
	}

	@AfterAll
	static void afterAll() {
		MONGO_DB.stop();
		MONGO_DB.close();
		KAFKA_CONTAINER.stop();
		KAFKA_CONTAINER.close();
	}

	@Test
	void submitDocument_WithValidData_ShouldSucceed() {
		StepVerifier.create(verificationService.submitDocument(VENDOR_ID, VerificationDocument.DocumentType.ID_CARD, "http://example.com/id.pdf"))
				.assertNext(document -> {
					assertThat(document).isNotNull();
					assertThat(document.getVendorId()).isEqualTo(VENDOR_ID);
					assertThat(document.getDocumentType()).isEqualTo(VerificationDocument.DocumentType.ID_CARD);
					assertThat(document.getStatus()).isEqualTo(VerificationDocument.VerificationStatus.PENDING);
				})
				.verifyComplete();
	}

	@Test
	void submitDocument_MissingDocumentType_ShouldFail() {
		StepVerifier.create(verificationService.submitDocument(VENDOR_ID, null, DOCUMENT_URL))
				.expectErrorMatches(e -> e instanceof ValidationException && e.getMessage().contains("Document type is required"))
				.verify();
	}

	@Test
	void submitDocument_MissingDocumentUrl_ShouldFail() {
		StepVerifier.create(verificationService.submitDocument(VENDOR_ID, VerificationDocument.DocumentType.ID_CARD, ""))
				.expectErrorMatches(e -> e instanceof ValidationException && e.getMessage().contains("Document URL is required"))
				.verify();
	}

	@Test
	void submitDocument_NonExistingVendor_ShouldFail() {
		UUID nonExistingVendorId = UUID.randomUUID();
		StepVerifier.create(verificationService.submitDocument(nonExistingVendorId, VerificationDocument.DocumentType.ID_CARD, DOCUMENT_URL))
				.expectErrorMatches(e -> e instanceof VendorNotFoundException)
				.verify();
	}

	@Test
	void getDocument_ExistingDocument_ShouldReturnDocument() {
		StepVerifier.create(verificationService.getDocument(DOCUMENT_ID))
				.assertNext(document -> {
					assertThat(document).isNotNull();
					assertThat(document.getId()).isEqualTo(DOCUMENT_ID);
				})
				.verifyComplete();
	}

	@Test
	void getDocument_NonExistingDocument_ShouldFail() {
		UUID nonExistingDocumentId = UUID.randomUUID();
		StepVerifier.create(verificationService.getDocument(nonExistingDocumentId))
				.expectErrorMatches(e -> e instanceof DocumentNotFoundException)
				.verify();
	}

	@Test
	void getVendorDocuments_ShouldReturnAllDocuments() {
		StepVerifier.create(verificationService.getVendorDocuments(VENDOR_ID).collectList())
				.assertNext(documents -> {
					assertThat(documents).hasSize(1);
					assertThat(documents.getFirst().getId()).isEqualTo(DOCUMENT_ID);
				})
				.verifyComplete();
	}

	@Test
	void getVendorDocuments_NonExistingVendor_ShouldFail() {
		UUID nonExistingVendorId = UUID.randomUUID();
		StepVerifier.create(verificationService.getVendorDocuments(nonExistingVendorId))
				.expectErrorMatches(e -> e instanceof VendorNotFoundException)
				.verify();
	}

	@Test
	void reviewDocument_ValidReview_ShouldSucceed() {
		StepVerifier.create(verificationService.reviewDocument(DOCUMENT_ID, VerificationDocument.VerificationStatus.APPROVED, "Approved"))
				.assertNext(document -> {
					assertThat(document).isNotNull();
					assertThat(document.getStatus()).isEqualTo(VerificationDocument.VerificationStatus.APPROVED);
				})
				.verifyComplete();

		ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "vendor.verification.completed.event", Duration.ofSeconds(5));

		assertThat(record).isNotNull();
		assertThat(record.value()).contains("VendorVerificationCompletedEvent");
	}

	@Test
	void reviewDocument_NonExistingDocument_ShouldFail() {
		UUID nonExistingDocumentId = UUID.randomUUID();
		StepVerifier.create(verificationService.reviewDocument(nonExistingDocumentId, VerificationDocument.VerificationStatus.APPROVED, "Approved"))
				.expectErrorMatches(e -> e instanceof DocumentNotFoundException)
				.verify();
	}
}
