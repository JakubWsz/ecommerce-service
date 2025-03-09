package pl.ecommerce.vendor.domain.model;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.UUID;

import static pl.ecommerce.vendor.domain.model.VerificationDocument.VerificationStatus.*;

import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;

@ToString
@SuperBuilder
@Getter
@Document(collection = "verification_documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationDocument extends BaseEntity {

	@Indexed
	@Field("vendorId")
	private UUID vendorId;

	@Field("documentType")
	private DocumentType documentType;

	@Field("documentUrl")
	private String documentUrl;

	@Setter
	@Field("status")
	private VerificationStatus status;

	@Setter
	@Field("reviewNotes")
	private String reviewNotes;

	@Field("submittedAt")
	private LocalDateTime submittedAt;

	@Setter
	@Field("reviewedAt")
	private LocalDateTime reviewedAt;

	@Setter
	@Field("reviewedBy")
	private UUID reviewedBy;

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

	public boolean canTransitionTo(VerificationStatus newStatus) {
		if (this.status != PENDING) {
			return false;
		}

		return newStatus != PENDING;
	}

	public enum DocumentType {
		BUSINESS_LICENSE, ID_CARD, TAX_CERTIFICATE, BANK_STATEMENT, PROOF_OF_ADDRESS
	}

	public enum VerificationStatus {
		PENDING, APPROVED, REJECTED
	}
}