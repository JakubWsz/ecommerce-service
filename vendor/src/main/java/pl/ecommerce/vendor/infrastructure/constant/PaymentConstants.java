package pl.ecommerce.vendor.infrastructure.constant;

public interface PaymentConstants extends CommonErrorConstants, CommonLogConstants {
	String ERROR_PAYMENT_NOT_FOUND = "Payment not found: ";
	String ERROR_PAYMENT_ALREADY_PROCESSED = "Payment already processed";
	String ERROR_CANNOT_PROCESS_PAYMENT = "Cannot process payment for inactive vendor";

	String LOG_PAYMENT_PROCESSED = "Payment processed: {}, {}";
}