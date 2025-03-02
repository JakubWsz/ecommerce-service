package pl.ecommerce.customer.api.dto;

public record GeoLocationDataDto(
		String country,
		String city,
		String voivodeship,
		String postalCode
) {}