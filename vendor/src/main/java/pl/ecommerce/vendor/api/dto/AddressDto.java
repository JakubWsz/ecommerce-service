package pl.ecommerce.vendor.api.dto;

import lombok.Builder;

@Builder
public record AddressDto(
		String street,
		String buildingNumber,
		String apartmentNumber,
		String city,
		String state,
		String postalCode,
		String country,
		String addressType
) {
}
