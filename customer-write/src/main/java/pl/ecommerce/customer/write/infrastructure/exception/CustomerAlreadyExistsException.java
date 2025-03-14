package pl.ecommerce.customer.write.infrastructure.exception;

public class CustomerAlreadyExistsException extends CustomerException {
	public CustomerAlreadyExistsException(String email) {
		super("Customer with email already exists: " + email);
	}

	public CustomerAlreadyExistsException(String email, String traceID) {
		super("Customer with email already exists: " + email, traceID);
	}
}