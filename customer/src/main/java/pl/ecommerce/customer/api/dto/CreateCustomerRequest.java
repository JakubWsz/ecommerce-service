package pl.ecommerce.customer.api.dto;

import java.util.List;

public record CreateCustomerRequest(
		PersonalDataDto personalData,
		List<AddressDto> addresses
) {}
