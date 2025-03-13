package pl.ecommerce.customer.domain.exceptions;

public class InvalidCustomerDataException extends CustomerException {
	public InvalidCustomerDataException(String message) {
		super(message);
	}
}
