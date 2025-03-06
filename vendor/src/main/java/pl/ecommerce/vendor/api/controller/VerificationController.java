package pl.ecommerce.vendor.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.ecommerce.vendor.api.mapper.DocumentMapper;
import pl.ecommerce.vendor.api.dto.DocumentRequest;
import pl.ecommerce.vendor.api.dto.DocumentResponse;
import pl.ecommerce.vendor.domain.model.VerificationDocument;
import pl.ecommerce.vendor.domain.service.VerificationService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Vendor Verification", description = "Endpoints for managing vendor verification documents")
@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

	private final VerificationService verificationService;

	@Operation(summary = "Submit document", description = "Submits a verification document for a vendor")
	@PostMapping("/{vendorId}/verification/documents")
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<DocumentResponse> submitDocument(
			@PathVariable String vendorId,
			@RequestBody DocumentRequest request) {
		return verificationService.submitDocument(
						UUID.fromString(vendorId),
						request.documentType(),
						request.documentUrl())
				.map(DocumentMapper::toResponse);
	}

	@Operation(summary = "Review document", description = "Reviews a submitted verification document")
	@PutMapping("/verification/documents/{documentId}/review")
	public Mono<DocumentResponse> reviewDocument(
			@PathVariable String documentId,
			@RequestParam String status,
			@RequestParam(required = false) String notes) {
		return verificationService.reviewDocument(
						UUID.fromString(documentId),
						VerificationDocument.VerificationStatus.valueOf(status),
						notes)
				.map(DocumentMapper::toResponse);
	}

	@Operation(summary = "Get document", description = "Gets a verification document by ID")
	@GetMapping("/verification/documents/{documentId}")
	public Mono<DocumentResponse> getDocument(
			@PathVariable String documentId) {
		return verificationService.getDocument(UUID.fromString(documentId))
				.map(DocumentMapper::toResponse);
	}

	@Operation(summary = "Get vendor documents", description = "Gets all verification documents for a vendor")
	@GetMapping("/{vendorId}/verification/documents")
	public Flux<DocumentResponse> getVendorDocuments(@PathVariable String vendorId) {
		return verificationService.getVendorDocuments(UUID.fromString(vendorId))
				.map(DocumentMapper::toResponse);
	}
}