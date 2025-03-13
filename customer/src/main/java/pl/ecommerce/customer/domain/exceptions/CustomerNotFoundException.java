package pl.ecommerce.customer.domain.exceptions;

import java.util.UUID;

public class CustomerNotFoundException extends CustomerException {
	public CustomerNotFoundException(UUID customerId) {
		super("Customer not found with ID: " + customerId);
	}

	public CustomerNotFoundException(String message) {
		super(message);
	}
}
