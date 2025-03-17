package pl.ecommerce.customer.write.infrastructure.exception;

import java.util.UUID;

public class CustomerNotFoundException extends CustomerException {
	public CustomerNotFoundException(UUID customerId) {
		super("Customer not found with ID: " + customerId);
	}

	public CustomerNotFoundException(String message) {
		super(message);
	}

	public CustomerNotFoundException(String message, String traceId) {
		super(message,traceId);
	}
}
