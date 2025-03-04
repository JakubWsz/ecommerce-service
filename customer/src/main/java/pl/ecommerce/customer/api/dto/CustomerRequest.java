package pl.ecommerce.customer.api.dto;

import java.util.List;

public record CustomerRequest(
		PersonalDataDto personalData,
		List<AddressDto> addresses
) {}