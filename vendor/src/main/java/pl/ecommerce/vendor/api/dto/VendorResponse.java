package pl.ecommerce.vendor.api.dto;

import lombok.Builder;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record VendorResponse(
		UUID id,
		String name,
		String description,
		String email,
		String phone,
		String businessName,
		String taxId,
		AddressDto businessAddress,
		String bankAccountDetails,
		String status,
		String verificationStatus,
		MonetaryAmount commissionRate,
		LocalDateTime registrationDate,
		List<CategoryAssignmentResponse> categories,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		Boolean active
) {
}
