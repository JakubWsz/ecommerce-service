package pl.ecommerce.vendor.write.infrastructure.exception;

import lombok.Getter;

@Getter
public class VendorAlreadyExistsException extends RuntimeException {
	private final String traceId;

	public VendorAlreadyExistsException(String message, String traceId) {
		super(message);
		this.traceId = traceId;
	}

}