package pl.ecommerce.customer.infrastructure.utils;

public class CustomerMessagesUtils {
	public static final String LOG_REGISTERING_CUSTOMER = "Registering new customer with email: {}";
	public static final String LOG_CUSTOMER_EXISTS = "Customer with email {} already exists";
	public static final String LOG_CUSTOMER_REGISTERED = "Customer registered successfully with id: {}";
	public static final String LOG_ERROR_SAVING_CUSTOMER = "Error occurred while saving customer: {}";
	public static final String LOG_CUSTOMER_FOUND = "Customer found: {}";
	public static final String LOG_FETCHING_CUSTOMER = "Fetching customer with id: {}";
	public static final String LOG_FETCHING_ALL_CUSTOMERS = "Fetching all active customers";
	public static final String LOG_UPDATING_CUSTOMER = "Updating customer with id: {}";
	public static final String LOG_CUSTOMER_UPDATED = "Customer updated successfully: {}";
	public static final String LOG_DELETING_CUSTOMER = "Deleting (deactivating) customer with id: {}";
	public static final String LOG_CUSTOMER_DEACTIVATED = "Customer deactivated successfully: {}";
	public static final String LOG_GEOLOCATION_ERROR = "Could not fetch geolocation data: {}";

	public static final String ERROR_CUSTOMER_NOT_FOUND = "Customer not found with id: ";
	public static final String ERROR_FETCHING_CUSTOMER = "Error fetching customer";
	public static final String ERROR_FAILED_TO_SAVE_CUSTOMER = "Failed to save customer";
	public static final String ERROR_FAILED_TO_REGISTER_CUSTOMER = "Custom error message: Failed to register customer. Reason: ";

	private CustomerMessagesUtils(){

	}
}
