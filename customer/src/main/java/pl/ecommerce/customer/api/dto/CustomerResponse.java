package pl.ecommerce.customer.api.dto;

import java.util.List;
import java.util.UUID;

public record CustomerResponse(
		UUID id,
		boolean active,
		String registrationIp,
		String createdAt,
		String updatedAt,
		PersonalDataDto personalData,
		List<AddressDto> addresses,
		GeoLocationDataDto geoLocationData
) {}
