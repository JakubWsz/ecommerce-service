package pl.ecommerce.vendor.read.infrastructure.projector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.vendor.*;
import pl.ecommerce.vendor.read.domain.VendorReadModel;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class VendorEventProjectorHelper {

	public Mono<VendorReadModel> createVendorFromRegistrationEvent(VendorRegisteredEvent event) {
		VendorReadModel vendor = VendorReadModel.builder()
				.id(event.getVendorId())
				.name(event.getName())
				.businessName(event.getBusinessName())
				.taxId(event.getTaxId())
				.email(event.getEmail())
				.phone(event.getPhone())
				.legalForm(event.getLegalForm())
				.status(event.getStatus())
				.verified(false)
				.commissionRate(event.getCommissionRate())
				.createdAt(event.getTimestamp())
				.updatedAt(event.getTimestamp())
				.lastUpdatedAt(event.getTimestamp())
				.build();

		if (event.getInitialCategories() != null && !event.getInitialCategories().isEmpty()) {
			vendor.setCategories(event.getInitialCategories().stream()
					.map(categoryId -> VendorReadModel.CategoryAssignment.builder()
							.categoryId(categoryId)
							.assignedAt(event.getTimestamp())
							.build())
					.collect(Collectors.toList()));
		}

		return Mono.just(vendor);
	}

	public Mono<VendorReadModel> applyVendorUpdates(VendorReadModel vendor, VendorUpdatedEvent event) {
		Map<String, Object> changes = event.getChanges();

		if (changes.containsKey("name")) {
			vendor.setName((String) changes.get("name"));
		}

		if (changes.containsKey("businessName")) {
			vendor.setBusinessName((String) changes.get("businessName"));
		}

		if (changes.containsKey("phone")) {
			vendor.setPhone((String) changes.get("phone"));
		}

		if (changes.containsKey("contactPersonName")) {
			vendor.setContactPersonName((String) changes.get("contactPersonName"));
		}

		if (changes.containsKey("contactPersonEmail")) {
			vendor.setContactPersonEmail((String) changes.get("contactPersonEmail"));
		}

		vendor.setUpdatedAt(event.getTimestamp());
		vendor.setLastUpdatedAt(event.getTimestamp());

		return Mono.just(vendor);
	}

	public Mono<VendorReadModel> addCategory(VendorReadModel vendor, VendorCategoryAssignedEvent event) {
		VendorReadModel.CategoryAssignment newCategory = VendorReadModel.CategoryAssignment.builder()
				.categoryId(event.getCategoryId())
				.categoryName(event.getCategoryName())
				.assignedAt(event.getTimestamp())
				.build();

		if (vendor.getCategories() == null) {
			vendor.setCategories(java.util.Collections.singletonList(newCategory));
		} else {
			vendor.getCategories().add(newCategory);
		}

		vendor.setUpdatedAt(event.getTimestamp());
		vendor.setLastUpdatedAt(event.getTimestamp());

		return Mono.just(vendor);
	}

	public Mono<VendorReadModel> removeCategory(VendorReadModel vendor, VendorCategoryRemovedEvent event) {
		if (vendor.getCategories() != null) {
			vendor.setCategories(vendor.getCategories().stream()
					.filter(category -> !category.getCategoryId().equals(event.getCategoryId()))
					.collect(Collectors.toList()));
		}

		vendor.setUpdatedAt(event.getTimestamp());
		vendor.setLastUpdatedAt(event.getTimestamp());

		return Mono.just(vendor);
	}

}