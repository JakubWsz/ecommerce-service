package pl.ecommerce.vendor.api.dto;

import jakarta.validation.constraints.Email;
import lombok.Builder;

@Builder
public record VendorUpdateRequest(
		String name,
		String description,
		@Email(message = "Invalid email format")
		String email,
		String phone,
		String businessName,
		String taxId,
		AddressDto businessAddress,
		String bankAccountDetails
) {
}
