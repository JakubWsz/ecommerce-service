package pl.ecommerce.vendor.api.dto;

import lombok.Builder;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CategoryAssignmentResponse(
		 UUID id,
		 UUID vendorId,
		 UUID categoryId,
		 String categoryName,
		 String status,
		 MonetaryAmount categoryCommissionRate,
		 LocalDateTime assignedAt,
		 LocalDateTime createdAt,
		 LocalDateTime updatedAt
) {
}
