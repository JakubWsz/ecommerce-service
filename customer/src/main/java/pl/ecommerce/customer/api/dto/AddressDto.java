package pl.ecommerce.customer.api.dto;

public record AddressDto(
		String street,
		String buildingNumber,
		String apartmentNumber,
		String city,
		String state,
		String postalCode,
		String country,
		boolean isDefault,
		String addressType
) {}