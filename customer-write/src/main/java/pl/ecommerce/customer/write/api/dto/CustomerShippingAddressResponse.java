package pl.ecommerce.customer.write.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Schema(description = "Response containing customer's shipping address details")
@Builder
public record CustomerShippingAddressResponse(
		@Schema(description = "Customer ID", example = "e7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a")
		UUID customerId,
		@Schema(description = "Shipping address ID", example = "a7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a")
		UUID addressId,
		@Schema(description = "Building number", example = "123")
		String buildingNumber,
		@Schema(description = "Apartment number", example = "45")
		String apartmentNumber,
		@Schema(description = "Street", example = "Main St")
		String street,
		@Schema(description = "City", example = "New York")
		String city,
		@Schema(description = "Postal code", example = "10001")
		String postalCode,
		@Schema(description = "Country", example = "USA")
		String country,
		@Schema(description = "Trace identifier", example = "12345678-1234-1234-1234-1234567890ab")
		String traceId
) {
}
