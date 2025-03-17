package pl.ecommerce.customer.write.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pl.ecommerce.commons.model.customer.CustomerConsents;

@Schema(description = "Request to register a new customer")
public record CustomerRegistrationRequest(
		@NotBlank @Email @Schema(description = "Customer's email", example = "john.doe@example.com")
		String email,
		@NotBlank @Schema(description = "Customer's first name", example = "John")
		String firstName,
		@NotBlank @Schema(description = "Customer's last name", example = "Doe")
		String lastName,
		@NotBlank @Schema(description = "Customer's phone number", example = "+1234567890")
		String phoneNumber,
		@NotBlank @Schema(description = "Customer's password", example = "P@ssw0rd")
		String password,
		@NotNull @Schema(description = "Customer consents")
		CustomerConsents consents
) {
}
