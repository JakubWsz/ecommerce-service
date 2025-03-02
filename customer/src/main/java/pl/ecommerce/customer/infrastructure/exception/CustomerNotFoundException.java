package pl.ecommerce.customer.infrastructure.exception;

public class CustomerNotFoundException extends RuntimeException {
	public CustomerNotFoundException(String message) {
		super(message);
	}

	public static CustomerNotFoundException throwEx(String message) {
		throw new CustomerNotFoundException(message);
	}
}