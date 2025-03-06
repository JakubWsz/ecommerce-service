package pl.ecommerce.vendor.infrastructure.utils;

public class VendorServiceConstants {

	public static final String LOG_REGISTERING_VENDOR = "Registering vendor: {}";
	public static final String LOG_VENDOR_REGISTERED = "Vendor registered with ID: {}";
	public static final String LOG_ERROR_SAVING_VENDOR = "Error saving vendor: {}";
	public static final String LOG_FETCHING_VENDOR = "Fetching vendor with ID: {}";
	public static final String LOG_VENDOR_FOUND = "Vendor found: {}";
	public static final String ERROR_VENDOR_NOT_FOUND = "Vendor not found: ";
	public static final String PAYMENT_ALREADY_PROCESSED = "Payment already processed";
	public static final String CANNOT_PROCESS_PAYMENT = "Cannot process payment for inactive vendor";
	public static final String PAYMENT_NOT_FOUND = "Payment not found: ";
	public static final String ERROR_FETCHING_VENDOR = "Error fetching vendor: {}";
	public static final String LOG_FETCHING_ALL_VENDORS = "Fetching all active vendors";
	public static final String LOG_UPDATING_VENDOR = "Updating vendor with ID: {}";
	public static final String LOG_VENDOR_UPDATED = "Vendor updated with ID: {}";
	public static final String LOG_UPDATING_VERIFICATION_STATUS = "Updating verification status for vendor ID: {}, Status: {}";
	public static final String LOG_VERIFICATION_UPDATED = "Verification status updated for vendor ID: {}, Status: {}";
	public static final String LOG_DEACTIVATING_VENDOR = "Deactivating vendor with ID: {}";
	public static final String LOG_VENDOR_DEACTIVATED = "Vendor deactivated with ID: {}";
	public static final String LOG_VENDOR_EXISTS = "Vendor with email {} already exists";
	public static final String LOG_PAYMENT_PROCESSED = "Payment processed: {}, {}";
	public static final String LOG_PROCESSING_PAYMENT = "Payment started: {}";
	public static final String LOG_ASSIGNING_CATEGORY = "Assigning category started: categoryId: {}, vendorId: {}";
	public static final String LOG_CATEGORY_ASSIGNED = " Category assigned with id: {} and vendor id: {}";
	public static final String LOG_REMOVING_CATEGORY = " Remove category with id: {} and vendor id: {}";

	private VendorServiceConstants() {
	}
}
