package pl.ecommerce.vendor.infrastructure;

import pl.ecommerce.commons.model.CategoryAssignment;
import pl.ecommerce.commons.event.vendor.*;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.model.VendorPayment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VendorEventUtils {
	private VendorEventUtils() {
	}

	public static VendorPaymentProcessedEvent createVendorPaymentProcessedEvent(Vendor vendor, VendorPayment savedPayment) {
		return VendorPaymentProcessedEvent.builder()
				.correlationId(UUID.randomUUID())
				.vendorId(vendor.getId())
				.paymentId(savedPayment.getId())
				.price(savedPayment.getAmount().getNumberStripped())
				.currencyUnit(savedPayment.getAmount().getCurrency().getCurrencyCode())
				.paymentDate(savedPayment.getPaymentDate())
				.build();
	}

	public static VendorRegisteredEvent createVendorRegisteredEvent(Vendor savedVendor) {
		return VendorRegisteredEvent.builder()
				.correlationId(UUID.randomUUID())
				.vendorId(savedVendor.getId())
				.name(savedVendor.getName())
				.email(savedVendor.getEmail())
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
				.verificationStatus(vendor.getVerificationStatus().name())
				.verificationTimestamp(LocalDateTime.now())
				.build();
	}

	public static VendorUpdatedEvent createVendorUpdatedEvent(UUID vendorId, Map<String, Object> changes) {
		return VendorUpdatedEvent.builder()
				.correlationId(UUID.randomUUID())
				.vendorId(vendorId)
				.changes(changes)
				.build();
	}

	public static VendorCategoriesAssignedEvent createVendorCategoriesAssignedEvent(UUID vendorId, List<CategoryAssignment> categories) {
		return VendorCategoriesAssignedEvent.builder()
				.correlationId(UUID.randomUUID())
				.vendorId(vendorId)
				.categories(categories)
				.build();
	}
}
