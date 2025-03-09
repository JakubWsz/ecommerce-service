package pl.ecommerce.vendor.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VendorRequest(
		@NotBlank(message = "Name is required")
		String name,
		String description,
		@NotBlank(message = "Email is required")
		@Email(message = "Invalid email format")
		String email,
		String phone,
		@NotBlank(message = "Business name is required")
		String businessName,
		String taxId,
		AddressDto businessAddress,
		String bankAccountDetails,
		@NotNull(message = "GDPR consent is required")
		Boolean gdprConsent
) {
}
