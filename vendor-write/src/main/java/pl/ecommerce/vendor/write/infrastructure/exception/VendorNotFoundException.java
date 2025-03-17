package pl.ecommerce.vendor.write.infrastructure.exception;

import java.util.UUID;

public class VendorNotFoundException extends RuntimeException {

	public VendorNotFoundException(String message) {
		super(message);
	}

	public VendorNotFoundException(UUID vendorId) {
		super("Vendor not found with ID: " + vendorId);
	}
}