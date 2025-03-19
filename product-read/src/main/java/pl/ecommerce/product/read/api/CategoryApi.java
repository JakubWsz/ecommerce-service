package pl.ecommerce.product.read.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.product.read.api.dto.CategoryResponse;
import pl.ecommerce.product.read.api.dto.CategorySummary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Categories", description = "Read operations for product categories")
@RequestMapping("/api/v1/categories")
public interface CategoryApi {

	@Operation(summary = "Get category by ID", description = "Returns a category by its ID")
	@GetMapping("/{id}")
	Mono<ResponseEntity<CategoryResponse>> getCategoryById(
			@PathVariable UUID id,
			ServerWebExchange exchange);

	@Operation(summary = "Get category by slug", description = "Returns a category by its slug")
	@GetMapping("/slug/{slug}")
	Mono<ResponseEntity<CategoryResponse>> getCategoryBySlug(
			@PathVariable String slug,
			ServerWebExchange exchange);

	@Operation(summary = "Get all root categories", description = "Returns all top-level categories")
	@GetMapping("/roots")
	Mono<ResponseEntity<Flux<CategorySummary>>> getRootCategories(
			ServerWebExchange exchange);

	@Operation(summary = "Get subcategories", description = "Returns subcategories of a specific category")
	@GetMapping("/{id}/subcategories")
	Mono<ResponseEntity<Flux<CategorySummary>>> getSubcategories(
			@PathVariable UUID id,
			ServerWebExchange exchange);

	@Operation(summary = "Get category tree", description = "Returns the complete category tree")
	@GetMapping("/tree")
	Mono<ResponseEntity<Flux<CategoryResponse>>> getCategoryTree(
			ServerWebExchange exchange);

	@Operation(summary = "Search categories", description = "Searches categories by name")
	@GetMapping("/search")
	Mono<ResponseEntity<Flux<CategorySummary>>> searchCategories(
			@RequestParam String query,
			ServerWebExchange exchange);
}
