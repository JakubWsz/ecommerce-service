package pl.ecommerce.customer.write.infrastructure.exception;

public class ConcurrencyException extends RuntimeException {
	public ConcurrencyException(String message) {
		super(message);
	}
}
