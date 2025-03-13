package pl.ecommerce.customer.api.dto;

import lombok.*;

@Builder
public record CustomerRegistrationRequest( String email,
		String firstName,
		String lastName,
		String phoneNumber,
		String password,
		CustomerConsents consents) {
}