package pl.ecommerce.vendor.api.mapper;

import pl.ecommerce.vendor.api.dto.DocumentResponse;
import pl.ecommerce.vendor.domain.model.VerificationDocument;

public class DocumentMapper {

	private DocumentMapper() {
	}

	public static DocumentResponse toResponse(VerificationDocument document) {
		return DocumentResponse.builder()
				.vendorId(document.getVendorId())
				.documentType(String.valueOf(document.getDocumentType()))
				.documentUrl(document.getDocumentUrl())
				.status(String.valueOf(document.getStatus()))
				.reviewNotes(document.getReviewNotes())
				.submittedAt(document.getSubmittedAt())
				.reviewedAt(document.getReviewedAt())
				.createdAt(document.getCreatedAt())
				.updatedAt(document.getUpdatedAt())
				.build();
	}
}
