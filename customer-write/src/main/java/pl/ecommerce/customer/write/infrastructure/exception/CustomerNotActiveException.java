package pl.ecommerce.customer.write.infrastructure.exception;

import java.util.UUID;

public class CustomerNotActiveException extends CustomerException {
	public CustomerNotActiveException(UUID customerId) {
		super("Customer is not active: " + customerId);
	}
}
