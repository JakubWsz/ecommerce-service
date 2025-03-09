package pl.ecommerce.vendor.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ecommerce.vendor.domain.model.Address;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.model.VerificationDocument;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.domain.repository.VerificationDocumentRepository;
import pl.ecommerce.vendor.infrastructure.exception.DocumentNotFoundException;
import pl.ecommerce.vendor.infrastructure.exception.ValidationException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pl.ecommerce.vendor.domain.model.VerificationDocument.DocumentType.BUSINESS_LICENSE;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

	@Mock
	private VerificationDocumentRepository documentRepository;

	@Mock
	private VendorService vendorService;

	@InjectMocks
	private VerificationService verificationService;

	private UUID vendorId;
	private UUID documentId;
	private Vendor vendor;
	private VerificationDocument document;
	private String documentUrl = "http://example.com/license.pdf";

	@BeforeEach
	void setUp() {
		vendorId = UUID.randomUUID();
		documentId = UUID.randomUUID();

		Address businessAddress = Address.builder()
				.street("123 Main St")
				.buildingNumber("1")
				.city("Test City")
				.state("Test State")
				.postalCode("12345")
				.country("Test Country")
				.build();

		vendor = Vendor.builder()
				.id(vendorId)
				.email("test.vendor@example.com")
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

		document = VerificationDocument.builder()
				.id(documentId)
				.vendorId(vendorId)
				.documentType(BUSINESS_LICENSE)
				.documentUrl(documentUrl)
				.status(VerificationDocument.VerificationStatus.PENDING)
				.submittedAt(LocalDateTime.now())
				.build();
	}

	@Test
	void submitDocument_success() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.just(vendor));
		when(documentRepository.save(any(VerificationDocument.class))).thenReturn(Mono.just(document));

		StepVerifier.create(verificationService.submitDocument(vendorId, BUSINESS_LICENSE, documentUrl))
				.expectNext(document)
				.verifyComplete();

		verify(vendorService).getVendorById(vendorId);
		verify(documentRepository).save(any(VerificationDocument.class));
	}

	@Test
	void submitDocument_vendorNotFound() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.empty());

		StepVerifier.create(verificationService.submitDocument(vendorId, BUSINESS_LICENSE, documentUrl))
				.expectError(VendorNotFoundException.class)
				.verify();

		verify(vendorService).getVendorById(vendorId);
		verify(documentRepository, never()).save(any(VerificationDocument.class));
	}

	@Test
	void submitDocument_nullDocumentType() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.just(vendor));

		StepVerifier.create(verificationService.submitDocument(vendorId, null, documentUrl))
				.expectError(ValidationException.class)
				.verify();

		verify(documentRepository, never()).save(any(VerificationDocument.class));
	}

	@Test
	void submitDocument_nullDocumentUrl() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.just(vendor));

		StepVerifier.create(verificationService.submitDocument(vendorId, BUSINESS_LICENSE, null))
				.expectError(ValidationException.class)
				.verify();

		verify(documentRepository, never()).save(any(VerificationDocument.class));
	}

	@Test
	void submitDocument_emptyDocumentUrl() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.just(vendor));

		StepVerifier.create(verificationService.submitDocument(vendorId, BUSINESS_LICENSE, " "))
				.expectError(ValidationException.class)
				.verify();

		verify(documentRepository, never()).save(any(VerificationDocument.class));
	}


	@Test
	void reviewDocument_approveSuccess() {
		when(documentRepository.findById(documentId)).thenReturn(Mono.just(document));
		when(documentRepository.save(any(VerificationDocument.class))).thenReturn(Mono.just(document));
		when(documentRepository.findByVendorId(vendorId)).thenReturn(Flux.just(document));
		when(vendorService.updateVerificationStatus(any(UUID.class), any(Vendor.VerificationStatus.class))).thenReturn(Mono.just(vendor));

		StepVerifier.create(verificationService.reviewDocument(documentId, VerificationDocument.VerificationStatus.APPROVED, "Looks good"))
				.expectNextMatches(doc ->
						doc.getStatus() == VerificationDocument.VerificationStatus.APPROVED &&
								"Looks good".equals(doc.getReviewNotes()) &&
								doc.getReviewedAt() != null)
				.verifyComplete();

		verify(documentRepository).findById(documentId);
		verify(documentRepository).save(any(VerificationDocument.class));
		verify(documentRepository).findByVendorId(vendorId);
		verify(vendorService).updateVerificationStatus(eq(vendorId), eq(Vendor.VerificationStatus.VERIFIED));
	}

	@Test
	void reviewDocument_rejectSuccess() {
		when(documentRepository.findById(documentId)).thenReturn(Mono.just(document));
		when(documentRepository.save(any(VerificationDocument.class))).thenReturn(Mono.just(document));
		when(documentRepository.findByVendorId(vendorId)).thenReturn(Flux.just(document));
		when(vendorService.updateVerificationStatus(any(UUID.class), any(Vendor.VerificationStatus.class))).thenReturn(Mono.just(vendor));

		StepVerifier.create(verificationService.reviewDocument(documentId, VerificationDocument.VerificationStatus.REJECTED, "Invalid document"))
				.expectNextMatches(doc ->
						doc.getStatus() == VerificationDocument.VerificationStatus.REJECTED &&
								"Invalid document".equals(doc.getReviewNotes()) &&
								doc.getReviewedAt() != null)
				.verifyComplete();

		verify(documentRepository).findById(documentId);
		verify(documentRepository).save(any(VerificationDocument.class));
		verify(documentRepository).findByVendorId(vendorId);
		verify(vendorService).updateVerificationStatus(eq(vendorId), eq(Vendor.VerificationStatus.REJECTED));
	}

	@Test
	void reviewDocument_documentNotFound() {
		when(documentRepository.findById(documentId)).thenReturn(Mono.empty());

		StepVerifier.create(verificationService.reviewDocument(documentId, VerificationDocument.VerificationStatus.APPROVED, "Looks good"))
				.expectError(DocumentNotFoundException.class)
				.verify();

		verify(documentRepository).findById(documentId);
		verify(documentRepository, never()).save(any(VerificationDocument.class));
		verify(vendorService, never()).updateVerificationStatus(any(UUID.class), any(Vendor.VerificationStatus.class));
	}

	@Test
	void reviewDocument_invalidStatus() {
		when(documentRepository.findById(documentId)).thenReturn(Mono.just(document));

		StepVerifier.create(verificationService.reviewDocument(documentId, VerificationDocument.VerificationStatus.PENDING, "Note"))
				.expectError(ValidationException.class)
				.verify();

		verify(documentRepository, never()).save(any(VerificationDocument.class));
	}

	@Test
	void reviewDocument_alreadyReviewed() {
		VerificationDocument reviewedDoc = VerificationDocument.builder()
				.id(documentId)
				.vendorId(vendorId)
				.documentType(BUSINESS_LICENSE)
				.documentUrl(documentUrl)
				.status(VerificationDocument.VerificationStatus.APPROVED)
				.submittedAt(LocalDateTime.now())
				.reviewedAt(LocalDateTime.now())
				.reviewNotes("Already approved")
				.build();

		when(documentRepository.findById(documentId)).thenReturn(Mono.just(reviewedDoc));

		StepVerifier.create(verificationService.reviewDocument(documentId, VerificationDocument.VerificationStatus.REJECTED, "Reject it"))
				.expectError(ValidationException.class)
				.verify();

		verify(documentRepository).findById(documentId);
		verify(documentRepository, never()).save(any(VerificationDocument.class));
	}

	@Test
	void reviewDocument_multipleDocumentsAllApproved() {
		VerificationDocument doc1 = document;

		VerificationDocument doc2 = VerificationDocument.builder()
				.id(UUID.randomUUID())
				.vendorId(vendorId)
				.documentType(VerificationDocument.DocumentType.TAX_CERTIFICATE)
				.documentUrl("http://example.com/tax.pdf")
				.status(VerificationDocument.VerificationStatus.APPROVED)
				.submittedAt(LocalDateTime.now())
				.reviewedAt(LocalDateTime.now())
				.build();

		when(documentRepository.findById(documentId)).thenReturn(Mono.just(doc1));
		when(documentRepository.save(any(VerificationDocument.class))).thenAnswer(invocation -> {
			VerificationDocument saved = invocation.getArgument(0);
			saved.setStatus(VerificationDocument.VerificationStatus.APPROVED);
			return Mono.just(saved);
		});
		when(documentRepository.findByVendorId(vendorId)).thenReturn(Flux.fromIterable(List.of(doc1, doc2)));
		when(vendorService.updateVerificationStatus(any(UUID.class), any(Vendor.VerificationStatus.class))).thenReturn(Mono.just(vendor));

		StepVerifier.create(verificationService.reviewDocument(documentId, VerificationDocument.VerificationStatus.APPROVED, "Approved"))
				.expectNextMatches(doc -> doc.getStatus() == VerificationDocument.VerificationStatus.APPROVED)
				.verifyComplete();

		verify(vendorService).updateVerificationStatus(vendorId, Vendor.VerificationStatus.VERIFIED);
	}

	@Test
	void reviewDocument_multipleDocumentsOneRejected() {
		VerificationDocument doc1 = document;

		VerificationDocument doc2 = VerificationDocument.builder()
				.id(UUID.randomUUID())
				.vendorId(vendorId)
				.documentType(VerificationDocument.DocumentType.TAX_CERTIFICATE)
				.documentUrl("http://example.com/tax.pdf")
				.status(VerificationDocument.VerificationStatus.APPROVED)
				.submittedAt(LocalDateTime.now())
				.reviewedAt(LocalDateTime.now())
				.build();

		when(documentRepository.findById(documentId)).thenReturn(Mono.just(doc1));
		when(documentRepository.save(any(VerificationDocument.class))).thenAnswer(invocation -> {
			VerificationDocument saved = invocation.getArgument(0);
			saved.setStatus(VerificationDocument.VerificationStatus.REJECTED);
			return Mono.just(saved);
		});
		when(documentRepository.findByVendorId(vendorId)).thenReturn(Flux.fromIterable(List.of(doc1, doc2)));
		when(vendorService.updateVerificationStatus(any(UUID.class), any(Vendor.VerificationStatus.class))).thenReturn(Mono.just(vendor));

		StepVerifier.create(verificationService.reviewDocument(documentId, VerificationDocument.VerificationStatus.REJECTED, "Rejected"))
				.expectNextMatches(doc -> doc.getStatus() == VerificationDocument.VerificationStatus.REJECTED)
				.verifyComplete();

		verify(vendorService).updateVerificationStatus(vendorId, Vendor.VerificationStatus.REJECTED);
	}


	@Test
	void getDocument_success() {
		when(documentRepository.findById(documentId)).thenReturn(Mono.just(document));

		StepVerifier.create(verificationService.getDocument(documentId))
				.expectNext(document)
				.verifyComplete();

		verify(documentRepository).findById(documentId);
	}

	@Test
	void getDocument_notFound() {
		when(documentRepository.findById(documentId)).thenReturn(Mono.empty());

		StepVerifier.create(verificationService.getDocument(documentId))
				.expectError(DocumentNotFoundException.class)
				.verify();

		verify(documentRepository).findById(documentId);
	}


	@Test
	void getVendorDocuments_success() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.just(vendor));
		when(documentRepository.findByVendorId(vendorId)).thenReturn(Flux.just(document));

		StepVerifier.create(verificationService.getVendorDocuments(vendorId))
				.expectNext(document)
				.verifyComplete();

		verify(vendorService).getVendorById(vendorId);
		verify(documentRepository).findByVendorId(vendorId);
	}

	@Test
	void getVendorDocuments_vendorNotFound() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.empty());

		StepVerifier.create(verificationService.getVendorDocuments(vendorId))
				.expectError(VendorNotFoundException.class)
				.verify();
	}

	@Test
	void getVendorDocuments_noDocuments() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.just(vendor));
		when(documentRepository.findByVendorId(vendorId)).thenReturn(Flux.empty());

		StepVerifier.create(verificationService.getVendorDocuments(vendorId))
				.verifyComplete();

		verify(vendorService).getVendorById(vendorId);
		verify(documentRepository).findByVendorId(vendorId);
	}
}