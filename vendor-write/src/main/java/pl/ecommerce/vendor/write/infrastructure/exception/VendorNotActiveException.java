package pl.ecommerce.vendor.write.infrastructure.exception;

import java.util.UUID;

public class VendorNotActiveException extends RuntimeException {

	public VendorNotActiveException(UUID vendorId) {
		super("Vendor with ID " + vendorId + " is not in ACTIVE state");
	}
}