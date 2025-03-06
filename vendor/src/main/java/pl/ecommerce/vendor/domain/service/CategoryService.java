package pl.ecommerce.vendor.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.domain.model.Category;
import pl.ecommerce.vendor.domain.model.CategoryAssignment;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.CategoryAssignmentRepository;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.infrastructure.client.ProductServiceClient;
import pl.ecommerce.vendor.infrastructure.exception.CategoryAssignmentException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import static pl.ecommerce.vendor.infrastructure.utils.VendorServiceConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

	private final CategoryAssignmentRepository categoryAssignmentRepository;
	private final VendorRepository vendorRepository;
	private final ProductServiceClient productServiceClient;
	private final EventPublisher eventPublisher;

	@Transactional
	public Mono<CategoryAssignment> assignCategory(UUID vendorId, String categoryId, MonetaryAmount commissionRate) {
		log.info(LOG_ASSIGNING_CATEGORY, categoryId, vendorId);

		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.flatMap(vendor -> validateAndAssignCategory(vendor, categoryId, commissionRate));
	}

	public Flux<CategoryAssignment> getVendorCategories(UUID vendorId) {
		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.thenMany(categoryAssignmentRepository.findByVendorId(vendorId));
	}

	public Flux<CategoryAssignment> getVendorActiveCategories(UUID vendorId) {
		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.thenMany(categoryAssignmentRepository
						.findByVendorIdAndStatus(vendorId, CategoryAssignment.CategoryAssignmentStatus.ACTIVE));
	}

	@Transactional
	public Mono<CategoryAssignment> updateCategoryStatus(UUID vendorId, String categoryId,
														 CategoryAssignment.CategoryAssignmentStatus status) {
		return handleErrorIfTrue(isInvalidStatus(status))
				.then(categoryAssignmentRepository.findByVendorIdAndCategoryId(vendorId, categoryId)
						.switchIfEmpty(Mono.error(new CategoryAssignmentException("Category assignment not found")))
						.flatMap(categoryAssignment -> updateStatus(categoryAssignment, status)));
	}

	@Transactional
	public Mono<Void> removeCategory(UUID vendorId, String categoryId) {
		log.info(LOG_REMOVING_CATEGORY, categoryId, vendorId);

		return categoryAssignmentRepository.findByVendorIdAndCategoryId(vendorId, categoryId)
				.switchIfEmpty(Mono.error(new CategoryAssignmentException("Category assignment not found")))
				.flatMap(categoryAssignmentRepository::delete);
	}

	private Mono<CategoryAssignment> validateAndAssignCategory(Vendor vendor, String categoryId,
															   MonetaryAmount commissionRate) {
		boolean isActive = !vendor.isActive();
		return handleErrorIfFalse(isActive, "Cannot assign category to inactive vendor")
				.then(checkCategoryAssignment(vendor.getId(), categoryId))
				.flatMap(categoryExists -> fetchCategoryDetails(categoryId))
				.flatMap(categoryDetails ->
						saveCategoryAssignment(vendor, commissionRate, categoryDetails));
	}

	private boolean isInvalidStatus(CategoryAssignment.CategoryAssignmentStatus status) {
		return !EnumSet.of(
						CategoryAssignment.CategoryAssignmentStatus.ACTIVE,
						CategoryAssignment.CategoryAssignmentStatus.INACTIVE)
				.contains(status);
	}

	private Mono<CategoryAssignment> updateStatus(CategoryAssignment assignment,
												  CategoryAssignment.CategoryAssignmentStatus status) {
		assignment.setStatus(status);
		assignment.setUpdatedAt(LocalDateTime.now());
		return categoryAssignmentRepository.save(assignment);
	}

	private Mono<Boolean> checkCategoryAssignment(UUID vendorId, String categoryId) {
		return categoryAssignmentRepository.existsByVendorIdAndCategoryId(vendorId, categoryId)
				.flatMap(this::handleErrorIfTrue)
				.then(productServiceClient.categoryExists(categoryId));
	}

	private Mono<ProductServiceClient.CategoryResponse> fetchCategoryDetails(String categoryId) {
		return productServiceClient.categoryExists(categoryId)
				.flatMap(categoryExists ->
						handleErrorIfFalse(categoryExists, "Category does not exist: " + categoryId))
				.then(productServiceClient.getCategoryDetails(categoryId));
	}

	private Mono<CategoryAssignment> saveCategoryAssignment(Vendor vendor, MonetaryAmount commissionRate,
															ProductServiceClient.CategoryResponse categoryResponse) {

		var assignment = createCategoryAssignment(vendor, commissionRate, categoryResponse);
		return categoryAssignmentRepository.save(assignment)
				.doOnSuccess(savedAssignment -> {
					//todo publish categoryAssignedEvent
					log.info(LOG_CATEGORY_ASSIGNED,
							savedAssignment.getCategory().getId(), savedAssignment.getVendorId());
				});
	}

	private static CategoryAssignment createCategoryAssignment(Vendor vendor, MonetaryAmount commissionRate,
															   ProductServiceClient.CategoryResponse categoryResponse) {
		return CategoryAssignment.builder()
				.id(UUID.randomUUID())
				.vendorId(vendor.getId())
				.category(toCategory(categoryResponse))
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.categoryCommissionRate(commissionRate)
				.assignedAt(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	private static Category toCategory(ProductServiceClient.CategoryResponse categoryResponse) {
		return Category.builder()
				.id(categoryResponse.id())
				.name(categoryResponse.name())
				.description(categoryResponse.description())
				.build();
	}

	private <T> Mono<T> handleErrorIfTrue(boolean condition) {
		return condition ?
				Mono.error(new CategoryAssignmentException("Category already assigned to vendor")) : Mono.empty();
	}

	private <T> Mono<T> handleErrorIfFalse(boolean condition, String errorMessage) {
		return !condition ? Mono.error(new CategoryAssignmentException(errorMessage)) : Mono.empty();
	}
}
