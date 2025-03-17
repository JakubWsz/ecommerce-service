package pl.ecommerce.customer.write.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update an existing customer")
public record CustomerUpdateRequest(
		@NotBlank @Email @Schema(description = "Customer's email", example = "john.doe@example.com")
		String email,
		@NotBlank @Schema(description = "Customer's first name", example = "John")
		String firstName,
		@NotBlank @Schema(description = "Customer's last name", example = "Doe")
		String lastName,
		@NotBlank @Schema(description = "Customer's phone number", example = "+1234567890")
		String phoneNumber
) {
}