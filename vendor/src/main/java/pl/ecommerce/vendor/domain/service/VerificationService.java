package pl.ecommerce.vendor.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.model.VerificationDocument;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.domain.repository.VerificationDocumentRepository;
import pl.ecommerce.vendor.infrastructure.exception.DocumentNotFoundException;
import pl.ecommerce.vendor.infrastructure.exception.ValidationException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

	private final VerificationDocumentRepository documentRepository;
	private final VendorRepository vendorRepository;
	private final VendorService vendorService;

	@Transactional
	public Mono<VerificationDocument> submitDocument(UUID vendorId, String documentType, String documentUrl) {
		return Mono.justOrEmpty(validateDocumentInputs(documentType, documentUrl))
				.then(vendorRepository.findById(vendorId)
						.switchIfEmpty(Mono.error(new VendorNotFoundException("Vendor not found: " + vendorId))))
				.flatMap(vendor -> createAndSaveDocument(vendorId, documentType, documentUrl));
	}

	@Transactional
	public Mono<VerificationDocument> reviewDocument(UUID documentId, VerificationDocument.VerificationStatus status, String notes) {
		return validateReviewStatus(status)
				.then(documentRepository.findById(documentId)
						.switchIfEmpty(Mono.error(new DocumentNotFoundException("Document not found: " + documentId))))
				.flatMap(document -> processDocumentReview(document, status, notes));
	}

	public Mono<VerificationDocument> getDocument(UUID documentId) {
		return documentRepository.findById(documentId)
				.switchIfEmpty(Mono.error(new DocumentNotFoundException("Document not found: " + documentId)));
	}

	public Flux<VerificationDocument> getVendorDocuments(UUID vendorId) {
		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException("Vendor not found: " + vendorId)))
				.thenMany(documentRepository.findByVendorId(vendorId));
	}

	private Mono<Void> validateDocumentInputs(String documentType, String documentUrl) {
		if (documentType == null || documentType.trim().isEmpty()) {
			return Mono.error(new ValidationException("Document type is required"));
		}
		if (documentUrl == null || documentUrl.trim().isEmpty()) {
			return Mono.error(new ValidationException("Document URL is required"));
		}
		return Mono.empty();
	}

	private Mono<VerificationDocument> createAndSaveDocument(UUID vendorId, String documentType, String documentUrl) {
		LocalDateTime now = LocalDateTime.now();
		VerificationDocument document = VerificationDocument.builder()
				.id(UUID.randomUUID())
				.vendorId(vendorId)
				.documentType(VerificationDocument.DocumentType.valueOf(documentType))
				.documentUrl(documentUrl)
				.status(VerificationDocument.VerificationStatus.PENDING)
				.submittedAt(now)
				.createdAt(now)
				.updatedAt(now)
				.build();
		return documentRepository.save(document);
	}

	private Mono<Void> validateReviewStatus(VerificationDocument.VerificationStatus status) {
		if (!List.of(VerificationDocument.VerificationStatus.APPROVED, VerificationDocument.VerificationStatus.REJECTED).contains(status)) {
			return Mono.error(new ValidationException("Invalid status: " + status));
		}
		return Mono.empty();
	}

	private Mono<VerificationDocument> processDocumentReview(VerificationDocument document, VerificationDocument.VerificationStatus status, String notes) {
		if (!VerificationDocument.VerificationStatus.PENDING.equals(document.getStatus())) {
			return Mono.error(new ValidationException("Document already reviewed"));
		}

		document.setStatus(status);
		document.setReviewNotes(notes);
		document.setReviewedAt(LocalDateTime.now());
		document.setUpdatedAt(LocalDateTime.now());

		return documentRepository.save(document)
				.flatMap(savedDocument -> checkAllDocumentsReviewed(savedDocument.getVendorId()))
				.thenReturn(document);
	}

	private Mono<Void> checkAllDocumentsReviewed(UUID vendorId) {
		return documentRepository.findByVendorId(vendorId)
				.collectList()
				.flatMap(documents -> checkAllDocumentsReviewed(vendorId, documents));
	}

	private Mono<Void> checkAllDocumentsReviewed(UUID vendorId, List<VerificationDocument> documents) {
		boolean allApproved = documents.stream()
				.allMatch(doc -> VerificationDocument.VerificationStatus.APPROVED.equals(doc.getStatus()));

		Vendor.VendorVerificationStatus newStatus =
				allApproved ?
						Vendor.VendorVerificationStatus.VERIFIED :
						Vendor.VendorVerificationStatus.REJECTED;

		return vendorService.updateVerificationStatus(vendorId, newStatus)
				.then();
	}
}