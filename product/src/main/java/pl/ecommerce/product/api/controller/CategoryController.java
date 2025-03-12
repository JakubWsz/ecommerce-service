package pl.ecommerce.product.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.ecommerce.product.api.DtoMapper;
import pl.ecommerce.product.api.dto.CategoryResponse;
import pl.ecommerce.product.api.dto.CreateCategoryRequest;
import pl.ecommerce.product.api.dto.UpdateCategoryRequest;
import pl.ecommerce.product.domain.service.CategoryService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Categories", description = "Category Management API")
public class CategoryController {

	private final CategoryService categoryService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a new category")
	public Mono<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
		return categoryService.createCategory(request)
				.map(DtoMapper::map);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get category by ID")
	public Mono<CategoryResponse> getCategory(@PathVariable UUID id) {
		return categoryService.getCategory(id)
				.map(DtoMapper::map);
	}

	@GetMapping("/by-name")
	@Operation(summary = "Get category by name")
	public Mono<CategoryResponse> getCategoryByName(@RequestParam String name) {
		return categoryService.getCategoryByName(name)
				.map(DtoMapper::map);
	}

	@GetMapping("/{parentId}")
	@Operation(summary = "Get category by name")
	public Flux<CategoryResponse> getCategoriesByParentId(@PathVariable UUID parentId) {
		return categoryService.getCategoriesByParentId(parentId)
				.map(DtoMapper::map);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a category")
	public Mono<CategoryResponse> updateCategory(
			@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
		return categoryService.updateCategory(id, request)
				.map(DtoMapper::map);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a category")
	public Mono<Void> deleteCategory(@PathVariable UUID id) {
		return categoryService.deleteCategory(id);
	}

	@PatchMapping("/deactivate/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a category")
	public Mono<Void> deactivateCategory(@PathVariable UUID id) {
		return categoryService.deactivateCategory(id);
	}

	@PatchMapping("/activateCategory/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a category")
	public Mono<Void> activateCategory(@PathVariable UUID id) {
		return categoryService.activateCategory(id);
	}

	@GetMapping("/root")
	@Operation(summary = "Get all root categories")
	public Flux<CategoryResponse> getRootCategories() {
		return categoryService.getRootCategories()
				.map(DtoMapper::map);
	}

	@GetMapping
	@Operation(summary = "Get all active categories")
	public Flux<CategoryResponse> getAllCategories() {
		return categoryService.getAllActiveCategories()
				.map(DtoMapper::map);
	}
}