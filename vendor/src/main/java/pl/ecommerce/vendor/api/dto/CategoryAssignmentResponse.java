package pl.ecommerce.vendor.api.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CategoryAssignmentResponse(
		 UUID id,
		 UUID vendorId,
		 String categoryId,
		 String categoryName,
		 String status,
		 BigDecimal categoryCommissionRate,
		 LocalDateTime assignedAt,
		 LocalDateTime createdAt,
		 LocalDateTime updatedAt
) {
}
