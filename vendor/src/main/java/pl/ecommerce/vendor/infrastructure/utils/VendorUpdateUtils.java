package pl.ecommerce.vendor.infrastructure.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

import java.util.Map;

@Slf4j
public final class VendorUpdateUtils {

	private VendorUpdateUtils() {
	}

	public static <T> void updateFieldIfPresent(T newValue, Consumer<T> setter, String fieldName, Map<String, Object> changes) {
		if (newValue != null) {
			setter.accept(newValue);
			changes.put(fieldName, newValue);
		}
	}
}
