package pl.ecommerce.vendor.infrastructure.constant;

public interface CommonLogConstants {
	String LOG_ENTITY_CREATED = "{} created with ID: {}";
	String LOG_ENTITY_UPDATED = "{} updated with ID: {}";
	String LOG_ENTITY_DELETED = "{} deleted with ID: {}";
	String LOG_OPERATION_STARTED = "{} operation started for {}: {}";
	String LOG_OPERATION_COMPLETED = "{} operation completed for {}: {}";
	String LOG_ERROR = "Error during {}: {}";
}