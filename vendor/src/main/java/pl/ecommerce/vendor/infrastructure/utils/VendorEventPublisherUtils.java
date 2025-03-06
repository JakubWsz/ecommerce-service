package pl.ecommerce.vendor.infrastructure.utils;

import lombok.extern.slf4j.Slf4j;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.domain.model.CategoryAssignment;
import pl.ecommerce.vendor.domain.model.Vendor;
import reactor.core.publisher.Mono;

import java.util.Set;

import static pl.ecommerce.vendor.infrastructure.VendorEventUtils.*;

@Slf4j
public final class VendorEventPublisherUtils {

	private VendorEventPublisherUtils() {
	}

	public static void publishVendorStatusChangedEvent(EventPublisher eventPublisher, String reason, Vendor saved) {
		var event = createVendorStatusChangedEvent(saved, saved.getVendorStatus(), reason);
		eventPublisher.publish(event);
		log.info("Vendor status updated: {}", saved.getId());
	}

	public static void publishVendorVerificationEvent(EventPublisher eventPublisher, Vendor vendor) {
		var event = createVendorVerificationCompletedEvent(vendor);
		eventPublisher.publish(event);
		log.info("Vendor verification updated: {}", vendor.getId());
	}

	public static Mono<Vendor> publishVendorRegisteredEvent(EventPublisher eventPublisher, Vendor savedVendor, Set<CategoryAssignment> categories) {
		var event = createVendorRegisteredEvent(savedVendor, categories);
		eventPublisher.publish(event);
		return Mono.just(savedVendor);
	}
}

