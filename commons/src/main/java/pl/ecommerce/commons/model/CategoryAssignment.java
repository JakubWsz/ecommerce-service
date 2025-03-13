package pl.ecommerce.commons.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CategoryAssignment(
		UUID id,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		String createdBy,
		String updatedBy,
		UUID vendorId,
		Category category,
		CategoryAssignmentStatusDto status,
		BigDecimal categoryCommissionRate,
		String currencyUnit,
		LocalDateTime assignedAt,
		String statusChangeReason

) {
	public enum CategoryAssignmentStatusDto {
		ACTIVE, INACTIVE
	}

	@Builder
	public record Category(
			UUID id,
			String name,
			String description
	) {
	}
}
