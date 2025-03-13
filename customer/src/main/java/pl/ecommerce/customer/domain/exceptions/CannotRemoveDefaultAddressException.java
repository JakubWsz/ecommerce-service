package pl.ecommerce.customer.domain.exceptions;

import java.util.UUID;

public class CannotRemoveDefaultAddressException extends CustomerException {
	public CannotRemoveDefaultAddressException(UUID addressId) {
		super("Cannot remove default address: " + addressId);
	}
}
