package pl.ecommerce.vendor.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.model.CategoryAssignment;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.api.dto.CategoryAssignmentRequest;
import pl.ecommerce.vendor.domain.model.Category;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.CategoryAssignmentRepository;
import pl.ecommerce.vendor.infrastructure.client.ProductServiceClient;
import pl.ecommerce.vendor.infrastructure.exception.CategoryAssignmentException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static pl.ecommerce.vendor.infrastructure.VendorEventUtils.createVendorCategoriesAssignedEvent;
import static pl.ecommerce.vendor.infrastructure.constant.CategoryConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

	private final CategoryAssignmentRepository categoryAssignmentRepository;
	private final VendorService vendorService;
	private final ProductServiceClient productServiceClient;
	private final EventPublisher eventPublisher;

	@Transactional
	public Flux<pl.ecommerce.vendor.domain.model.CategoryAssignment> assignCategories(UUID vendorId, List<CategoryAssignmentRequest> request) {
		log.info(LOG_OPERATION_STARTED, "Category assignment", "vendor", vendorId);

		return vendorService.getVendorById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.flatMapMany(vendor -> validateAndAssignCategory(vendor, request))
				.doOnComplete(() -> log.info(LOG_OPERATION_COMPLETED, "Category assignment", "vendor", vendorId))
				.doOnError(e -> log.error(LOG_ERROR, "category assignment", e.getMessage(), e));
	}

	public Flux<pl.ecommerce.vendor.domain.model.CategoryAssignment> getVendorCategories(UUID vendorId) {
		log.debug(LOG_OPERATION_STARTED, "Fetching categories", "vendor", vendorId);

		return vendorService.getVendorById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.thenMany(categoryAssignmentRepository.findByVendorId(vendorId))
				.doOnComplete(() -> log.debug(LOG_OPERATION_COMPLETED, "Categories fetched", "vendor", vendorId));
	}

	public Flux<pl.ecommerce.vendor.domain.model.CategoryAssignment> getVendorActiveCategories(UUID vendorId) {
		log.debug(LOG_OPERATION_STARTED, "Fetching active categories", "vendor", vendorId);

		return vendorService.getVendorById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.thenMany(categoryAssignmentRepository
						.findByVendorIdAndStatus(vendorId, pl.ecommerce.vendor.domain.model.CategoryAssignment.CategoryAssignmentStatus.ACTIVE))
				.doOnComplete(() -> log.debug(LOG_OPERATION_COMPLETED, "Active categories fetched", "vendor", vendorId));
	}

	@Transactional
	public Mono<pl.ecommerce.vendor.domain.model.CategoryAssignment> updateCategoryStatus(UUID vendorId, UUID categoryId,
																						  pl.ecommerce.vendor.domain.model.CategoryAssignment.CategoryAssignmentStatus status) {
		log.info(LOG_OPERATION_STARTED, "Category status update", "vendor", vendorId);

		return handleErrorIfTrue(isInvalidStatus(status))
				.then(categoryAssignmentRepository.findByVendorIdAndCategoryId(vendorId, categoryId)
						.switchIfEmpty(Mono.error(new CategoryAssignmentException(ERROR_CATEGORY_ASSIGNMENT_NOT_FOUND)))
						.flatMap(categoryAssignment -> updateStatus(categoryAssignment, status)))
				.doOnSuccess(result -> log.info(LOG_ENTITY_UPDATED, "Category status", categoryId));
	}

	@Transactional
	public Mono<Void> removeCategory(UUID vendorId, UUID categoryId) {
		log.info(LOG_OPERATION_STARTED, "Category removal", "vendor", vendorId);

		return categoryAssignmentRepository.findByVendorIdAndCategoryId(vendorId, categoryId)
				.switchIfEmpty(Mono.error(new CategoryAssignmentException(ERROR_CATEGORY_ASSIGNMENT_NOT_FOUND)))
				.flatMap(categoryAssignmentRepository::delete)
				.doOnSuccess(v -> log.info(LOG_ENTITY_DELETED, "Category", categoryId));
	}

	private Flux<pl.ecommerce.vendor.domain.model.CategoryAssignment> validateAndAssignCategory(Vendor vendor, List<CategoryAssignmentRequest> requests) {
		return handleErrorIfFalse(vendor.getActive(), ERROR_CATEGORY_FOR_INACTIVE_VENDOR)
				.then(checkForExistingAssignments(vendor.getId(), requests))
				.then(fetchAndValidateCategoryDetails(extractCategoryIds(requests)))
				.flatMapMany(categoryResponses -> createCategoryAssignments(vendor, requests, categoryResponses));
	}

	private List<UUID> extractCategoryIds(List<CategoryAssignmentRequest> requests) {
		return requests.stream()
				.map(req -> UUID.fromString(req.categoryId()))
				.collect(Collectors.toList());
	}

	private Mono<Void> checkForExistingAssignments(UUID vendorId, List<CategoryAssignmentRequest> requests) {
		List<UUID> categoryIds = extractCategoryIds(requests);
		return checkCategoriesAssignment(vendorId, categoryIds)
				.flatMap(this::handleErrorIfTrue);
	}

	private Mono<List<ProductServiceClient.CategoryResponse>> fetchAndValidateCategoryDetails(List<UUID> categoryIds) {
		return productServiceClient.getCategories(categoryIds)
				.collectList()
				.flatMap(categoryResponses ->
						validateAllCategoriesExist(categoryResponses, categoryIds)
								.thenReturn(categoryResponses)
				);
	}

	private Mono<Void> validateAllCategoriesExist(List<ProductServiceClient.CategoryResponse> responses, List<UUID> requestedIds) {
		return handleErrorIfFalse(
				responses.size() == requestedIds.size(),
				ERROR_CATEGORIES_NOT_EXISTS
		);
	}

	private ProductServiceClient.CategoryResponse findCategoryResponseById(
			List<ProductServiceClient.CategoryResponse> responses,
			UUID categoryId) {

		return responses.stream()
				.filter(resp -> resp.id().equals(categoryId))
				.findFirst()
				.orElseThrow(() -> new CategoryAssignmentException(ERROR_CATEGORY_NOT_FOUND + categoryId));
	}

	private Mono<Boolean> checkCategoriesAssignment(UUID vendorId, List<UUID> categoryIds) {
		return Flux.fromIterable(categoryIds)
				.flatMap(categoryId -> categoryAssignmentRepository.existsByVendorIdAndCategoryId(vendorId, categoryId))
				.any(exists -> exists);
	}

	private boolean isInvalidStatus(pl.ecommerce.vendor.domain.model.CategoryAssignment.CategoryAssignmentStatus status) {
		return !EnumSet.of(
						pl.ecommerce.vendor.domain.model.CategoryAssignment.CategoryAssignmentStatus.ACTIVE,
						pl.ecommerce.vendor.domain.model.CategoryAssignment.CategoryAssignmentStatus.INACTIVE)
				.contains(status);
	}

	private Mono<pl.ecommerce.vendor.domain.model.CategoryAssignment> updateStatus(pl.ecommerce.vendor.domain.model.CategoryAssignment assignment,
																				   pl.ecommerce.vendor.domain.model.CategoryAssignment.CategoryAssignmentStatus status) {
		assignment.setStatus(status);
		return categoryAssignmentRepository.save(assignment);
	}

	private Flux<pl.ecommerce.vendor.domain.model.CategoryAssignment> createCategoryAssignments(
			Vendor vendor,
			List<CategoryAssignmentRequest> requests,
			List<ProductServiceClient.CategoryResponse> categoryResponses) {

		List<pl.ecommerce.vendor.domain.model.CategoryAssignment> assignments = requests.stream()
				.map(request -> createCategoryAssignment(vendor, categoryResponses, request))
				.toList();

		return saveAllCategoryAssignments(assignments, vendor.getId());
	}

	private Flux<pl.ecommerce.vendor.domain.model.CategoryAssignment> saveAllCategoryAssignments(List<pl.ecommerce.vendor.domain.model.CategoryAssignment> assignments, UUID vendorId) {
		return categoryAssignmentRepository.saveAll(assignments)
				.collectList()
				.doOnNext(savedAssignments -> {
					log.info(LOG_OPERATION_COMPLETED, "All category assignments saved", "vendor", vendorId);
					var categories = mapCategoryAssignments(assignments);
					publishVendorCategoriesAssignedEvent(vendorId, categories);
				})
				.flatMapMany(Flux::fromIterable);
	}

	private pl.ecommerce.vendor.domain.model.CategoryAssignment createCategoryAssignment(Vendor vendor, List<ProductServiceClient.CategoryResponse> categoryResponses, CategoryAssignmentRequest request) {
		UUID catId = UUID.fromString(request.categoryId());
		ProductServiceClient.CategoryResponse categoryResponse = findCategoryResponseById(categoryResponses, catId);
		return pl.ecommerce.vendor.domain.model.CategoryAssignment.create(vendor.getId(), toCategory(categoryResponse), request.commissionRate());
	}

	private static Category toCategory(ProductServiceClient.CategoryResponse categoryResponse) {
		return pl.ecommerce.vendor.domain.model.Category.builder()
				.id(categoryResponse.id())
				.name(categoryResponse.name())
				.description(categoryResponse.description())
				.build();
	}

	private static List<CategoryAssignment> mapCategoryAssignments(List<pl.ecommerce.vendor.domain.model.CategoryAssignment> assignments) {
		return assignments.stream()
				.map(CategoryService::map)
				.toList();
	}

	private static CategoryAssignment map(pl.ecommerce.vendor.domain.model.CategoryAssignment categoryAssignment) {
		return CategoryAssignment.builder()
				.id(categoryAssignment.getId())
				.createdAt(categoryAssignment.getCreatedAt())
				.updatedAt(categoryAssignment.getUpdatedAt())
				.createdBy(categoryAssignment.getCreatedBy())
				.updatedBy(categoryAssignment.getUpdatedBy())
				.vendorId(categoryAssignment.getVendorId())
				.category(mapCategory(categoryAssignment.getCategory()))
				.status(CategoryAssignment.CategoryAssignmentStatusDto.valueOf(categoryAssignment.getStatus().name()))
				.categoryCommissionRate(categoryAssignment.getCategoryCommissionRate().getNumberStripped())
				.currencyUnit(categoryAssignment.getCategoryCommissionRate().getCurrency().getCurrencyCode())
				.assignedAt(categoryAssignment.getAssignedAt())
				.statusChangeReason(categoryAssignment.getStatusChangeReason())
				.build();
	}

	private static CategoryAssignment.Category mapCategory(Category category) {
		return CategoryAssignment.Category.builder()
				.id(category.getId())
				.name(category.getName())
				.description(category.getDescription())
				.build();
	}

	public void publishVendorCategoriesAssignedEvent(UUID vendorId, List<CategoryAssignment> categories) {
		var event = createVendorCategoriesAssignedEvent(vendorId, categories);
		log.info("Categories {}, assigned for vendor: {}", categories, vendorId);
		eventPublisher.publish(event);
	}

	private <T> Mono<T> handleErrorIfTrue(boolean condition) {
		return condition ?
				Mono.error(new CategoryAssignmentException(ERROR_CATEGORY_ALREADY_ASSIGNED)) : Mono.empty();
	}

	private <T> Mono<T> handleErrorIfFalse(boolean condition, String errorMessage) {
		return !condition ? Mono.error(new CategoryAssignmentException(errorMessage)) : Mono.empty();
	}
}