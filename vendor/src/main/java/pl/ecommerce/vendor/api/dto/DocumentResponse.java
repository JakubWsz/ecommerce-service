package pl.ecommerce.vendor.api.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record DocumentResponse(
		UUID id,
		UUID vendorId,
		String documentType,
		String documentUrl,
		String status,
		String reviewNotes,
		LocalDateTime submittedAt,
		LocalDateTime reviewedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
