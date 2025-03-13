package pl.ecommerce.customer.domain.valueobjects;

import pl.ecommerce.customer.domain.exceptions.InvalidCustomerDataException;

public record PhoneNumber(String value) {
	public PhoneNumber {
		if (value == null || value.isBlank()) {
			throw new InvalidCustomerDataException("Phone number cannot be empty");
		}
	}
}
