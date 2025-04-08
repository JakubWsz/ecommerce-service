package pl.ecommerce.customer.write.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Response after registering a new customer")
public record CustomerRegistrationResponse(
		@Schema(description = "Customer ID", example = "e7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a")
		UUID id,
		@Schema(description = "Customer's email", example = "john.doe@example.com")
		String email,
		@Schema(description = "Customer's first name", example = "John")
		String firstName,
		@Schema(description = "Customer's last name", example = "Doe")
		String lastName
) {
}