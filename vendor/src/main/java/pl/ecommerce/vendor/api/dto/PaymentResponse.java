package pl.ecommerce.vendor.api.dto;

import lombok.Builder;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record PaymentResponse(
		UUID id,
		UUID vendorId,
		MonetaryAmount amount,
		String status,
		String paymentMethod,
		UUID referenceId,
		String notes,
		LocalDateTime paymentDate,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
