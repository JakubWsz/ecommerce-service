package pl.ecommerce.customer.infrastructure.exception;

public class CustomerAlreadyExistsException extends RuntimeException {
	public CustomerAlreadyExistsException(String message) {
		super(message);
	}

}