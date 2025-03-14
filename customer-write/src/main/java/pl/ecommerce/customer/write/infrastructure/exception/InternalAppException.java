package pl.ecommerce.customer.write.infrastructure.exception;

public class InternalAppException extends CustomerException {
	public InternalAppException(String message) {
		super(message);
	}

	public InternalAppException(String message, Throwable e) {
		super(message, e);
	}

}
