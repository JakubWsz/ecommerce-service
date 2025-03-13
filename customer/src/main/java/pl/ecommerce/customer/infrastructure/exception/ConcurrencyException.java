package pl.ecommerce.customer.infrastructure.exception;

public class ConcurrencyException extends RuntimeException {
	public ConcurrencyException(String message) {
		super(message);
	}
}
