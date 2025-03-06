package pl.ecommerce.vendor.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import javax.money.MonetaryAmount;

@Builder
public record CategoryAssignmentRequest(
		@NotBlank(message = "Category ID is required")
		String categoryId,
		@Positive(message = "Commission rate must be positive")
		MonetaryAmount commissionRate
) {
}
