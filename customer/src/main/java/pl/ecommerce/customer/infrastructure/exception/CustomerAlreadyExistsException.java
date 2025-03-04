package pl.ecommerce.customer.infrastructure.exception;

public class CustomerAlreadyExistsException extends RuntimeException {

	private static final String ERROR_CUSTOMER_ALREADY_EXISTS = "Customer with this email already exists";

	public CustomerAlreadyExistsException() {
		super(ERROR_CUSTOMER_ALREADY_EXISTS);
	}

}