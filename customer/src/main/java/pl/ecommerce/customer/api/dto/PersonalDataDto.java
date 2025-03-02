package pl.ecommerce.customer.api.dto;

public record PersonalDataDto(
		String email,
		String firstName,
		String lastName,
		String phoneNumber
) {}
