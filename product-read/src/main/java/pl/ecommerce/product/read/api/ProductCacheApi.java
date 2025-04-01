package pl.ecommerce.product.read.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.product.read.api.dto.CacheStatisticsResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Cache Management", description = "Administrative endpoints for product cache management")
@RequestMapping("/api/v1/admin/cache")
public interface ProductCacheApi {

	@Operation(summary = "Refresh product cache", description = "Refreshes cache for a specific product")
	@PostMapping("/products/{productId}/refresh")
	Mono<ResponseEntity<Boolean>> refreshProduct(
			@PathVariable UUID productId,
			ServerWebExchange exchange);

	@Operation(summary = "Invalidate product cache", description = "Removes a specific product from cache")
	@DeleteMapping("/products/{productId}")
	Mono<ResponseEntity<Boolean>> invalidateProduct(
			@PathVariable UUID productId,
			ServerWebExchange exchange);

	@Operation(summary = "Invalidate product cache by SKU", description = "Removes a specific product from cache using its SKU")
	@DeleteMapping("/products/sku/{sku}")
	Mono<ResponseEntity<Boolean>> invalidateProductBySku(
			@PathVariable String sku,
			ServerWebExchange exchange);

	@Operation(summary = "Refresh featured products", description = "Refreshes cache for all featured products")
	@PostMapping("/products/featured/refresh")
	Mono<ResponseEntity<Boolean>> refreshFeaturedProducts(
			ServerWebExchange exchange);

	@Operation(summary = "Clear all product cache", description = "Removes all products from cache")
	@DeleteMapping("/products")
	Mono<ResponseEntity<Boolean>> clearAllProductCache(
			ServerWebExchange exchange);

	@Operation(summary = "Get cache statistics", description = "Returns current cache performance statistics")
	@GetMapping("/statistics")
	Mono<ResponseEntity<CacheStatisticsResponse>> getCacheStatistics(
			ServerWebExchange exchange);
}