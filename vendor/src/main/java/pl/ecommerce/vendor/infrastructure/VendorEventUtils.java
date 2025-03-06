package pl.ecommerce.vendor.infrastructure;

import pl.ecommerce.commons.event.vendor.VendorPaymentProcessedEvent;
import pl.ecommerce.commons.event.vendor.VendorRegisteredEvent;
import pl.ecommerce.commons.event.vendor.VendorStatusChangedEvent;
import pl.ecommerce.commons.event.vendor.VendorVerificationCompletedEvent;
import pl.ecommerce.vendor.domain.model.CategoryAssignment;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.model.VendorPayment;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class VendorEventUtils {
	private VendorEventUtils() {
	}

	public static VendorPaymentProcessedEvent createVendorPaymentProcessedEvent(Vendor vendor, VendorPayment savedPayment) {
		return VendorPaymentProcessedEvent.builder()
				.correlationId(UUID.randomUUID())
				.vendorId(vendor.getId())
				.paymentId(savedPayment.getId())
				.amount(savedPayment.getAmount())
				.paymentDate(savedPayment.getPaymentDate())
				.build();
	}

	public static VendorRegisteredEvent createVendorRegisteredEvent(Vendor savedVendor, Set<CategoryAssignment> categories) {
		Set<Map<UUID, String>> categoriesList = categories.stream()
				.map(category ->
						Map.of(category.getCategory().getId(), category.getCategory().getName()))
				.collect(Collectors.toSet());

		return VendorRegisteredEvent.builder()
				.correlationId(UUID.randomUUID())
				.vendorId(savedVendor.getId())
				.name(savedVendor.getName())
				.email(savedVendor.getEmail())
				.productCategories(categoriesList)
				.status(savedVendor.getVendorStatus().name())
				.build();
	}

	public static VendorStatusChangedEvent createVendorStatusChangedEvent(Vendor saved, Vendor.VendorStatus existingVendorStatus, String reason) {
		return VendorStatusChangedEvent.builder()
				.correlationId(UUID.randomUUID())
				.vendorId(saved.getId())
				.oldStatus(existingVendorStatus.name())
				.newStatus(saved.getVendorStatus().name())
				.reason(reason)
				.build();
	}

	public static VendorVerificationCompletedEvent createVendorVerificationCompletedEvent(Vendor vendor) {
		return VendorVerificationCompletedEvent.builder()
				.correlationId(UUID.randomUUID())
				.vendorId(vendor.getId())
				.verificationStatus(vendor.getVerificationVendorStatus().name())
				.verificationTimestamp(LocalDateTime.now())
				.build();
	}
}
