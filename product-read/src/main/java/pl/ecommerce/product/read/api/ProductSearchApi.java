package pl.ecommerce.product.read.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.product.read.api.dto.ProductSummary;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Product Search", description = "Advanced search operations for products")
@RequestMapping("/api/v1/products/search")
public interface ProductSearchApi {

	@Operation(summary = "Advanced search products", description = "Search products with multiple filters and criteria")
	@GetMapping("/advanced")
	Mono<ResponseEntity<Page<ProductSummary>>> advancedSearch(
			@RequestParam(required = false) String query,
			@RequestParam(required = false) List<UUID> categories,
			@RequestParam(required = false) List<UUID> vendors,
			@RequestParam(required = false) List<String> brands,
			@RequestParam(required = false) @Parameter(description = "Minimum price") BigDecimal minPrice,
			@RequestParam(required = false) @Parameter(description = "Maximum price") BigDecimal maxPrice,
			@RequestParam(required = false) @Parameter(description = "Only show discounted products") Boolean onlyDiscounted,
			@RequestParam(required = false) @Parameter(description = "Only show products in stock") Boolean inStock,
			@RequestParam(required = false) @Parameter(description = "Attribute name to filter by") String attributeName,
			@RequestParam(required = false) @Parameter(description = "Attribute value to filter by") String attributeValue,
			@RequestParam(required = false) @Parameter(description = "Only show featured products") Boolean featured,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "name") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir,
			ServerWebExchange exchange);

	@Operation(summary = "Find similar products", description = "Find products similar to a given product")
	@GetMapping("/{productId}/similar")
	Mono<ResponseEntity<List<ProductSummary>>> findSimilarProducts(
			@PathVariable UUID productId,
			@RequestParam(defaultValue = "5") int limit,
			ServerWebExchange exchange);

	@Operation(summary = "Find products by price range", description = "Find products within specified price range")
	@GetMapping("/price-range")
	Mono<ResponseEntity<Page<ProductSummary>>> findByPriceRange(
			@RequestParam BigDecimal minPrice,
			@RequestParam BigDecimal maxPrice,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "price.currentPrice") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir,
			ServerWebExchange exchange);

	@Operation(summary = "Find products on sale", description = "Find products with discounts")
	@GetMapping("/on-sale")
	Mono<ResponseEntity<Page<ProductSummary>>> findProductsOnSale(
			@RequestParam(required = false) @Parameter(description = "Minimum discount percentage") Integer minDiscountPercentage,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "price.discountPercentage") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir,
			ServerWebExchange exchange);

	@Operation(summary = "Find products by attributes", description = "Find products matching specific attributes")
	@GetMapping("/by-attributes")
	Mono<ResponseEntity<Page<ProductSummary>>> findProductsByAttributes(
			@RequestParam Map<String, String> attributes,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "name") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir,
			ServerWebExchange exchange);

	@Operation(summary = "Find latest products", description = "Find the most recently added products")
	@GetMapping("/latest")
	Mono<ResponseEntity<List<ProductSummary>>> findLatestProducts(
			@RequestParam(defaultValue = "10") int limit,
			ServerWebExchange exchange);

	@Operation(summary = "Find popular products", description = "Find the most popular products")
	@GetMapping("/popular")
	Mono<ResponseEntity<List<ProductSummary>>> findPopularProducts(
			@RequestParam(defaultValue = "10") int limit,
			ServerWebExchange exchange);
}