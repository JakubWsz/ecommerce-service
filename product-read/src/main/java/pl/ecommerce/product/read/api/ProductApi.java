package pl.ecommerce.product.read.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.product.read.api.dto.ProductResponse;
import pl.ecommerce.product.read.api.dto.ProductSummary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Tag(name = "Products", description = "Read operations for product data")
@RequestMapping("/api/v1/products")
public interface ProductApi {

	@Operation(summary = "Get product by ID", description = "Returns a product by its ID")
	@GetMapping("/{id}")
	Mono<ResponseEntity<ProductResponse>> getProductById(
			@PathVariable UUID id,
			ServerWebExchange exchange);

	@Operation(summary = "Get product by SKU", description = "Returns a product by its SKU")
	@GetMapping("/sku/{sku}")
	Mono<ResponseEntity<ProductResponse>> getProductBySku(
			@PathVariable String sku,
			ServerWebExchange exchange);

	@Operation(summary = "Search products", description = "Searches products by name or description")
	@GetMapping("/search")
	Mono<ResponseEntity<Page<ProductSummary>>> searchProducts(
			@RequestParam String query,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "name") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir,
			ServerWebExchange exchange);

	@Operation(summary = "Get products by category", description = "Returns products in a specific category")
	@GetMapping("/category/{categoryId}")
	Mono<ResponseEntity<Page<ProductSummary>>> getProductsByCategory(
			@PathVariable UUID categoryId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "name") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir,
			ServerWebExchange exchange);

	@Operation(summary = "Get products by vendor", description = "Returns products by a specific vendor")
	@GetMapping("/vendor/{vendorId}")
	Mono<ResponseEntity<Page<ProductSummary>>> getProductsByVendor(
			@PathVariable UUID vendorId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "name") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir,
			ServerWebExchange exchange);

	@Operation(summary = "Get featured products", description = "Returns featured products")
	@GetMapping("/featured")
	Mono<ResponseEntity<Page<ProductSummary>>> getFeaturedProducts(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			ServerWebExchange exchange);

	@Operation(summary = "Filter products", description = "Filters products by multiple criteria")
	@GetMapping("/filter")
	Mono<ResponseEntity<Page<ProductSummary>>> filterProducts(
			@RequestParam(required = false) Set<UUID> categories,
			@RequestParam(required = false) Set<UUID> vendors,
			@RequestParam(required = false) UUID brandId,
			@RequestParam(required = false) @Parameter(description = "Minimum price") BigDecimal minPrice,
			@RequestParam(required = false) @Parameter(description = "Maximum price") BigDecimal maxPrice,
			@RequestParam(required = false) @Parameter(description = "Only show products in stock") Boolean inStock,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "name") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir,
			ServerWebExchange exchange);

	@Operation(summary = "Get related products", description = "Returns products related to a specific product")
	@GetMapping("/{id}/related")
	Mono<ResponseEntity<Flux<ProductSummary>>> getRelatedProducts(
			@PathVariable UUID id,
			@RequestParam(defaultValue = "5") int limit,
			ServerWebExchange exchange);
}
