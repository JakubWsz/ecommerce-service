package pl.ecommerce.customer.write.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Response after changing the customer's email")
public record CustomerEmailChangeResponse(
		@Schema(description = "Customer ID", example = "e7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a")
		UUID id,
		@Schema(description = "Trace identifier", example = "12345678-1234-1234-1234-1234567890ab")
		String traceId,
		@Schema(description = "New email address", example = "new.email@example.com")
		String email
) {
}