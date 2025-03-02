package pl.ecommerce.customer.infrastructure.exception;

public class InternalAppException extends RuntimeException {
	public InternalAppException(String message) {
		super(message);
	}

	public static InternalAppException throwEx(String message) {
		throw new InternalAppException(message);
	}
}
