package pl.ecommerce.customer.domain.exceptions;

import java.util.UUID;

public class CustomerNotActiveException extends CustomerException {
	public CustomerNotActiveException(UUID customerId) {
		super("Customer is not active: " + customerId);
	}
}
