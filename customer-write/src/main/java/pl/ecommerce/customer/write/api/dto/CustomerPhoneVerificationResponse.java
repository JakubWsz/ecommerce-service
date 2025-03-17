package pl.ecommerce.customer.write.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Response after verifying a customer's phone number")
public record CustomerPhoneVerificationResponse(
		@Schema(description = "Customer ID", example = "e7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a")
		UUID customerId,
		@Schema(description = "Trace identifier", example = "12345678-1234-1234-1234-1234567890ab")
		String traceId
) {
}