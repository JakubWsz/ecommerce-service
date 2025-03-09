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

import static java.util.Objects.isNull;
import static pl.ecommerce.vendor.infrastructure.constant.VerificationConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

	private final VerificationDocumentRepository documentRepository;
	private final VendorService vendorService;

	@Transactional
	public Mono<VerificationDocument> submitDocument(UUID vendorId, VerificationDocument.DocumentType documentType, String documentUrl) {
		log.info(LOG_OPERATION_STARTED, "Document submission", "vendor", vendorId);

		return validateDocumentInputs(documentType, documentUrl)
				.then(vendorService.getVendorById(vendorId)
						.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId))))
				.flatMap(vendor -> createAndSaveDocument(vendorId, documentType, documentUrl))
				.doOnSuccess(document -> log.info(LOG_ENTITY_CREATED, "Verification document", document.getId()))
				.onErrorMap(ValidationException.class, e -> {
					log.error(LOG_ERROR, "Validation failed", e.getMessage(), e);
					return new ValidationException(e.getMessage(), e);
				});
	}


	@Transactional
	public Mono<VerificationDocument> reviewDocument(UUID documentId, VerificationDocument.VerificationStatus status, String notes) {
		log.info(LOG_OPERATION_STARTED, "Document review", "document", documentId);

		return validateReviewStatus(status)
				.then(documentRepository.findById(documentId)
						.switchIfEmpty(Mono.error(new DocumentNotFoundException(ERROR_DOCUMENT_NOT_FOUND + documentId))))
				.flatMap(document -> processDocumentReview(document, status, notes))
				.doOnSuccess(document -> log.info(LOG_DOCUMENT_REVIEWED, document.getId(), document.getStatus()))
				.doOnError(e -> log.error(LOG_ERROR, "document review", e.getMessage(), e));
	}

	public Mono<VerificationDocument> getDocument(UUID documentId) {
		log.debug(LOG_OPERATION_STARTED, "Document retrieval", "document", documentId);

		return documentRepository.findById(documentId)
				.switchIfEmpty(Mono.error(new DocumentNotFoundException(ERROR_DOCUMENT_NOT_FOUND + documentId)))
				.doOnSuccess(document -> log.debug(LOG_OPERATION_COMPLETED, "Document retrieval", "document", documentId));
	}

	public Flux<VerificationDocument> getVendorDocuments(UUID vendorId) {
		log.debug(LOG_OPERATION_STARTED, "Vendor documents retrieval", "vendor", vendorId);

		return vendorService.getVendorById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.thenMany(documentRepository.findByVendorId(vendorId))
				.doOnComplete(() -> log.debug(LOG_OPERATION_COMPLETED, "Vendor documents retrieval", "vendor", vendorId));
	}

	private Mono<Void> validateDocumentInputs(VerificationDocument.DocumentType documentType, String documentUrl) {
		if (isNull(documentType) ) {
			log.warn("Document type is required but was empty");
			return Mono.error(new ValidationException(ERROR_DOCUMENT_TYPE_REQUIRED));
		}
		if (isNull(documentUrl) || documentUrl.trim().isEmpty()) {
			log.warn("Document URL is required but was empty");
			return Mono.error(new ValidationException(ERROR_DOCUMENT_URL_REQUIRED));
		}
		return Mono.empty();
	}

	private Mono<VerificationDocument> createAndSaveDocument(UUID vendorId, VerificationDocument.DocumentType documentType , String documentUrl) {
		log.debug("Creating document of type {} for vendor {}", documentType, vendorId);
		LocalDateTime now = LocalDateTime.now();
		VerificationDocument document = createVerificationDocument(vendorId, documentType, documentUrl, now);
		return documentRepository.save(document);
	}

	private Mono<Void> validateReviewStatus(VerificationDocument.VerificationStatus status) {
		if (!List.of(VerificationDocument.VerificationStatus.APPROVED, VerificationDocument.VerificationStatus.REJECTED).contains(status)) {
			log.warn("Invalid verification status: {}", status);
			return Mono.error(new ValidationException(ERROR_INVALID_STATUS + status));
		}
		return Mono.empty();
	}

	private Mono<VerificationDocument> processDocumentReview(VerificationDocument document, VerificationDocument.VerificationStatus status, String notes) {
		if (!VerificationDocument.VerificationStatus.PENDING.equals(document.getStatus())) {
			log.warn("Document already reviewed: {}", document.getId());
			return Mono.error(new ValidationException(ERROR_DOCUMENT_ALREADY_REVIEWED));
		}

		log.info("Processing review for document {} with status {}", document.getId(), status);
		document.setStatus(status);
		document.setReviewNotes(notes);
		document.setReviewedAt(LocalDateTime.now());

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

		log.info("All documents for vendor {} reviewed. Approved: {}", vendorId, allApproved);
		return vendorService.updateVerificationStatus(vendorId, resolveStatus(allApproved))
				.then(Mono.empty());
	}

	private static Vendor.VerificationStatus resolveStatus(boolean allApproved) {
		return allApproved ?
				Vendor.VerificationStatus.VERIFIED :
				Vendor.VerificationStatus.REJECTED;
	}

	private static VerificationDocument createVerificationDocument(UUID vendorId, VerificationDocument.DocumentType documentType , String documentUrl, LocalDateTime now) {
		return VerificationDocument.builder()
				.vendorId(vendorId)
				.documentType(documentType)
				.documentUrl(documentUrl)
				.status(VerificationDocument.VerificationStatus.PENDING)
				.submittedAt(now)
				.build();
	}
}