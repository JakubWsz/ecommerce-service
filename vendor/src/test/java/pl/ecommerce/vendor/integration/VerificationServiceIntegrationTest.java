package pl.ecommerce.vendor.integration;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.*;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import pl.ecommerce.vendor.domain.model.VerificationDocument;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.VerificationDocumentRepository;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.domain.service.VerificationService;
import pl.ecommerce.vendor.domain.service.VendorService;
import pl.ecommerce.vendor.infrastructure.exception.DocumentNotFoundException;
import pl.ecommerce.vendor.infrastructure.exception.ValidationException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import pl.ecommerce.vendor.integration.helper.KafkaTopics;
import pl.ecommerce.vendor.integration.helper.TestUtils;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(partitions = 1, topics = {"vendor.verification.completed.event"})
class VerificationServiceIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private VerificationDocumentRepository documentRepository;

	@Autowired
	private VendorRepository vendorRepository;

	@Autowired
	private VendorService vendorService;

	private VerificationService verificationService;

	private static final UUID VENDOR_ID = UUID.randomUUID();
	private static final UUID DOCUMENT_ID = UUID.randomUUID();
	private static final String DOCUMENT_URL = "http://example.com/document.pdf";
	private Vendor testVendor;
	private VerificationDocument testDocument;

	@BeforeAll
	static void setupClass() {
		setupKafkaConsumer(KafkaTopics.VERIFICATION_TOPICS);
		waitForKafkaReady(KafkaTopics.VERIFICATION_TOPICS);
	}

	@BeforeEach
	void setupBeforeEach() {
		MockitoAnnotations.openMocks(this);

		TestUtils.cleanRepositories(vendorRepository, null, documentRepository, null);

		testVendor = TestUtils.createTestVendor(VENDOR_ID, "vendor@example.com");

		testDocument = TestUtils.createTestDocument(
				DOCUMENT_ID,
				VENDOR_ID,
				VerificationDocument.DocumentType.TAX_CERTIFICATE,
				DOCUMENT_URL);

		vendorRepository.save(testVendor).block();
		documentRepository.save(testDocument).block();

		verificationService = new VerificationService(documentRepository, vendorService);
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

		ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(kafkaConsumer,
				KafkaTopics.VENDOR_VERIFICATION_COMPLETED, Duration.ofSeconds(5));

		assertThat(record).isNotNull();
		assertThat(record.value()).contains("VendorVerificationCompletedEvent");
	}

	@Test
	void reviewDocument_NonExistingDocument_ShouldFail() {
		UUID nonExistingDocumentId = UUID.randomUUID();

		StepVerifier.create(verificationService.reviewDocument(nonExistingDocumentId,
						VerificationDocument.VerificationStatus.APPROVED, "Approved"))
				.expectErrorMatches(e -> e instanceof DocumentNotFoundException)
				.verify();
	}
}