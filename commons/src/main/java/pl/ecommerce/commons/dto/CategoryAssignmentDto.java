package pl.ecommerce.commons.dto;

import lombok.Builder;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CategoryAssignmentDto(
		UUID id,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		String createdBy,
		String updatedBy,
		UUID vendorId,
		CategoryDto category,
		CategoryAssignmentStatusDto status,
		MonetaryAmount categoryCommissionRate,
		LocalDateTime assignedAt,
		String statusChangeReason

) {
	public enum CategoryAssignmentStatusDto {
		ACTIVE, INACTIVE
	}
	@Builder
	public record CategoryDto(
			UUID id,
			String name,
			String description
	) {
	}
}
