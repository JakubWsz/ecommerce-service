package pl.ecommerce.vendor.integration.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Klasa zawierająca stałe dla tematów Kafka używanych w testach
 */
public class KafkaTopics {

	public static final String VENDOR_REGISTERED = "vendor.registered.event";
	public static final String VENDOR_UPDATED = "vendor.updated.event";
	public static final String VENDOR_STATUS_CHANGED = "vendor.status.changed.event";
	public static final String VENDOR_VERIFICATION_COMPLETED = "vendor.verification.completed.event";

	public static final String PAYMENT_PROCESSED = "vendor.payment.processed.event";

	public static final String CATEGORIES_ASSIGNED = "vendor.categories.assigned.event";

	public static final List<String> ALL_TOPICS = Arrays.asList(
			VENDOR_REGISTERED,
			VENDOR_UPDATED,
			VENDOR_STATUS_CHANGED,
			VENDOR_VERIFICATION_COMPLETED,
			PAYMENT_PROCESSED,
			CATEGORIES_ASSIGNED
	);

	public static final List<String> VENDOR_TOPICS = Arrays.asList(
			VENDOR_REGISTERED,
			VENDOR_UPDATED,
			VENDOR_STATUS_CHANGED,
			VENDOR_VERIFICATION_COMPLETED
	);

	public static final List<String> PAYMENT_TOPICS = Collections.singletonList(
			PAYMENT_PROCESSED
	);

	public static final List<String> CATEGORY_TOPICS = Collections.singletonList(
			CATEGORIES_ASSIGNED
	);

	public static final List<String> VERIFICATION_TOPICS = Collections.singletonList(
			VENDOR_VERIFICATION_COMPLETED
	);
}