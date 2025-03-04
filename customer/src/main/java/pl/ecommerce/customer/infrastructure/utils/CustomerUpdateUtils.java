package pl.ecommerce.customer.infrastructure.utils;

import java.util.Map;
import java.util.function.Consumer;

public final class CustomerUpdateUtils {

	private CustomerUpdateUtils() {
	}

	public static <T> void updateFieldIfPresent(T newValue, Consumer<T> setter, String fieldName, Map<String, Object> changes) {
		if (newValue != null) {
			setter.accept(newValue);
			changes.put(fieldName, newValue);
		}
	}

	public static void updateConsentField(boolean newValue, boolean currentValue, Consumer<Boolean> setter,
										  String fieldName, Map<String, Object> changes) {
		updateConsentField(newValue, currentValue, setter, fieldName, changes, null);
	}

	public static void updateConsentField(boolean newValue, boolean currentValue, Consumer<Boolean> setter,
										  String fieldName, Map<String, Object> changes, Runnable additionalAction) {
		if (newValue != currentValue) {
			setter.accept(newValue);
			changes.put(fieldName, newValue);
			if (additionalAction != null) {
				additionalAction.run();
			}
		}
	}
}