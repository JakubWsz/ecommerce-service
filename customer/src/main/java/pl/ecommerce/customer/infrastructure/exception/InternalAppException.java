package pl.ecommerce.customer.infrastructure.exception;

public class InternalAppException extends RuntimeException {
	public InternalAppException(String message) {
		super(message);
	}

	public InternalAppException(String message, Throwable e) {
		super(message, e);
	}

}
