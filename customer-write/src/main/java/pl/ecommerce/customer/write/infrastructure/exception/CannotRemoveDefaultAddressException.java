package pl.ecommerce.customer.write.infrastructure.exception;

import java.util.UUID;

public class CannotRemoveDefaultAddressException extends CustomerException {
	public CannotRemoveDefaultAddressException(UUID addressId) {
		super("Cannot remove default address: " + addressId);
	}
}
