package pl.ecommerce.customer.api.dto;


import java.util.List;

public record UpdateCustomerRequest(
		PersonalDataDto personalData,
		List<AddressDto> addresses
) {}