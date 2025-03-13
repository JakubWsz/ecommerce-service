package pl.ecommerce.customer.domain.exceptions;

public class CustomerAlreadyExistsException extends CustomerException {
	public CustomerAlreadyExistsException(String email) {
		super("Customer with email already exists: " + email);
	}
}