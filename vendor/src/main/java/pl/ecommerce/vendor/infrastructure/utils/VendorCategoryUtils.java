package pl.ecommerce.vendor.infrastructure.utils;

import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.domain.model.Category;
import pl.ecommerce.vendor.domain.model.CategoryAssignment;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.CategoryAssignmentRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.isNull;
import static pl.ecommerce.vendor.infrastructure.utils.VendorEventPublisherUtils.publishVendorRegisteredEvent;

public final class VendorCategoryUtils {

	private VendorCategoryUtils() {
	}

	public static Mono<Vendor> assignCategoriesAndPublishEvent(Vendor savedVendor,
															   CategoryAssignmentRepository categoryAssignmentRepository,
															   EventPublisher eventPublisher) {
		if (isNull(savedVendor.getCategories())|| savedVendor.getCategories().isEmpty()) {
			return publishVendorRegisteredEvent(eventPublisher, savedVendor, Set.of());
		}

		List<CategoryAssignment> categoryAssignments = savedVendor.getCategories().stream()
				.map(category -> createCategoryAssignment(savedVendor, category))
				.toList();

		return categoryAssignmentRepository.saveAll(categoryAssignments)
				.collectList()
				.flatMap(assignments ->
						publishVendorRegisteredEvent(eventPublisher, savedVendor, new HashSet<>(assignments)))
				.thenReturn(savedVendor);
	}

	private static CategoryAssignment createCategoryAssignment(Vendor savedVendor, Category category) {
		return CategoryAssignment.builder()
				.id(UUID.randomUUID())
				.vendorId(savedVendor.getId())
				.category(category)
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.assignedAt(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}
}

