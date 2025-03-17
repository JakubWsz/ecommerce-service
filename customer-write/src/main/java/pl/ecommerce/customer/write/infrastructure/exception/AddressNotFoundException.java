package pl.ecommerce.customer.write.infrastructure.exception;

import java.util.UUID;

public class AddressNotFoundException extends CustomerException {
	public AddressNotFoundException(UUID addressId) {
		super("Address not found with ID: " + addressId);
	}
}