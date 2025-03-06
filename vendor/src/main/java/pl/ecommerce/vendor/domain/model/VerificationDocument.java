package pl.ecommerce.vendor.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

import static pl.ecommerce.vendor.domain.model.VerificationDocument.VerificationStatus.*;

@Builder
@Getter
@Document(collection = "verification_documents")
public class VerificationDocument {

	@Id
	private UUID id;
	private UUID vendorId;
	private DocumentType documentType;
	private String documentUrl;
	@Setter
	private VerificationStatus status;
	@Setter
	private String reviewNotes;
	private LocalDateTime submittedAt;
	@Setter
	private LocalDateTime reviewedAt;
	private LocalDateTime createdAt;
	@Setter
	private LocalDateTime updatedAt;


	public boolean isPending() {
		return PENDING.equals(status);
	}

	public boolean isApproved() {
		return APPROVED.equals(status);
	}

	public boolean isRejected() {
		return REJECTED.equals(status);
	}

	public boolean requiresReview() {
		return isPending() && (reviewNotes == null || reviewNotes.trim().isEmpty());
	}

	public enum DocumentType{
		BUSINESS_LICENSE, ID_CARD, TAX_CERTIFICATE
	}

	public enum VerificationStatus{
		PENDING, APPROVED, REJECTED
	}
}
