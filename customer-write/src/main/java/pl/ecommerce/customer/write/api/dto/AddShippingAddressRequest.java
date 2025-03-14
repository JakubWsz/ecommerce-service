package pl.ecommerce.customer.write.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to add a new shipping address")
public record AddShippingAddressRequest(
		@NotBlank @Schema(description = "Address type", example = "home")
		String addressType,
		@NotBlank @Schema(description = "Building number", example = "123")
		String buildingNumber,
		@Schema(description = "Apartment number", example = "45")
		String apartmentNumber,
		@NotBlank @Schema(description = "Street name", example = "Main St")
		String street,
		@NotBlank @Schema(description = "City", example = "New York")
		String city,
		@NotBlank @Schema(description = "Postal code", example = "10001")
		String postalCode,
		@NotBlank @Schema(description = "Country", example = "USA")
		String country,
		@Schema(description = "State", example = "NY")
		String state,
		@NotNull @Schema(description = "Flag indicating if this is the default address", example = "true")
		boolean isDefault
) {
}