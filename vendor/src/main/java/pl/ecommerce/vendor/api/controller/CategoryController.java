package pl.ecommerce.vendor.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.ecommerce.vendor.api.dto.CategoryAssignmentRequest;
import pl.ecommerce.vendor.api.mapper.CategoryAssignmentMapper;
import pl.ecommerce.vendor.api.dto.CategoryAssignmentResponse;
import pl.ecommerce.vendor.domain.model.CategoryAssignment;
import pl.ecommerce.vendor.domain.service.CategoryService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Vendor Categories", description = "Endpoints for managing vendor product categories")
@RestController
@RequestMapping("/api/v1/vendors/{vendorId}/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

	private final CategoryService categoryService;

	@Operation(summary = "Assign category", description = "Assigns a product category to a vendor")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<CategoryAssignmentResponse> assignCategory(
			@PathVariable String vendorId,
			@RequestBody CategoryAssignmentRequest request) {
		return categoryService.assignCategory(
						UUID.fromString(vendorId),
						request.categoryId(),
						request.commissionRate())
				.map(CategoryAssignmentMapper::toResponse);
	}

	@Operation(summary = "Get all categories", description = "Gets all categories assigned to a vendor")
	@GetMapping
	public Flux<CategoryAssignmentResponse> getVendorCategories(@PathVariable String vendorId) {
		return categoryService.getVendorCategories(UUID.fromString(vendorId))
				.map(CategoryAssignmentMapper::toResponse);
	}

	@Operation(summary = "Get active categories", description = "Gets only active categories assigned to a vendor")
	@GetMapping("/active")
	public Flux<CategoryAssignmentResponse> getVendorActiveCategories(@PathVariable String vendorId) {
		return categoryService.getVendorActiveCategories(UUID.fromString(vendorId))
				.map(CategoryAssignmentMapper::toResponse);
	}

	@Operation(summary = "Update category status", description = "Updates the status of a category assignment")
	@PutMapping("/{categoryId}/status")
	public Mono<CategoryAssignmentResponse> updateCategoryStatus(
			@PathVariable String vendorId,
			@PathVariable String categoryId,
			@RequestParam String status) {
		return categoryService.updateCategoryStatus(UUID.fromString(vendorId), categoryId, CategoryAssignment.CategoryAssignmentStatus.valueOf(status))
				.map(CategoryAssignmentMapper::toResponse);
	}

	@Operation(summary = "Remove category", description = "Removes a category assignment from a vendor")
	@DeleteMapping("/{categoryId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> removeCategory(
			@PathVariable String vendorId,
			@PathVariable String categoryId) {
		return categoryService.removeCategory(UUID.fromString(vendorId), categoryId);
	}
}

