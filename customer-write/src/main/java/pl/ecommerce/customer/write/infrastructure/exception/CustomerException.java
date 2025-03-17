package pl.ecommerce.customer.write.infrastructure.exception;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public abstract class CustomerException extends RuntimeException {

	private final String traceId;

	public CustomerException(String message) {
		super(message);
		this.traceId = null;
	}

	public CustomerException(String message, String traceId) {
		super(message);
		this.traceId = traceId;
	}

	public CustomerException(String message, Throwable cause) {
		super(message, cause);
		this.traceId = null;
	}

	public CustomerException(String message, Throwable cause, String traceId) {
		super(message, cause);
		this.traceId = traceId;
	}

	public boolean hasTraceId() {
		return traceId != null && !traceId.isEmpty();
	}
}