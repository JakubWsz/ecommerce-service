package pl.ecommerce.product.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import pl.ecommerce.product.api.DtoMapper;
import pl.ecommerce.product.api.dto.ProductCreateRequest;
import pl.ecommerce.product.api.dto.ProductResponse;
import pl.ecommerce.product.api.dto.ProductUpdateRequest;
import pl.ecommerce.product.domain.service.ProductService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product Management API")
public class ProductController {

	private final ProductService productService;

	@GetMapping("/{id}")
	@Operation(summary = "Get product by ID")
	public Mono<ProductResponse> getProductById(@PathVariable UUID id) {
		return productService.getProductById(id)
				.map(DtoMapper::map);
	}

	@GetMapping("/active")
	@Operation(summary = "Get all active products")
	public Flux<ProductResponse> getAllActiveProducts(Pageable pageable) {
		return productService.getAllActiveProducts(pageable)
				.map(DtoMapper::map);
	}

	@GetMapping("/vendor/{vendorId}")
	@Operation(summary = "Get products by vendor ID")
	public Flux<ProductResponse> getProductsByVendor(@PathVariable String vendorId, Pageable pageable) {
		return productService.getProductsByVendor(vendorId, pageable)
				.map(DtoMapper::map);
	}

	@GetMapping("/category/{categoryId}")
	@Operation(summary = "Get products by category ID")
	public Flux<ProductResponse> getProductsByCategory(@PathVariable UUID categoryId, Pageable pageable) {
		return productService.getProductsByCategory(categoryId, pageable)
				.map(DtoMapper::map);
	}

	@GetMapping("/search")
	@Operation(summary = "Search products by query")
	public Flux<ProductResponse> searchProducts(@RequestParam String query, Pageable pageable) {
		return productService.searchProducts(query, pageable)
				.map(DtoMapper::map);
	}

	@GetMapping("/count/active")
	@Operation(summary = "Count active products")
	public Mono<Long> countActiveProducts() {
		return productService.countActiveProducts();
	}

	@GetMapping("/count/category/{categoryId}")
	@Operation(summary = "Count products in a category")
	public Mono<Long> countProductsByCategory(@PathVariable UUID categoryId) {
		return productService.countProductsByCategory(categoryId);
	}

	@PostMapping
	@Operation(summary = "Create a new product")
	public Mono<ProductResponse> createProduct(@RequestBody ProductCreateRequest request) {
		return productService.createProduct(request)
				.map(DtoMapper::map);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a product")
	public Mono<ProductResponse> updateProduct(@PathVariable UUID id, @RequestBody ProductUpdateRequest request) {
		return productService.updateProduct(id, request)
				.map(DtoMapper::map);
	}

	@PatchMapping("/{id}/stock")
	@Operation(summary = "Update product stock")
	public Mono<ProductResponse> updateStock(@PathVariable UUID id, @RequestParam int stock) {
		return productService.updateProductStock(id, stock)
				.map(DtoMapper::map);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a product")
	public Mono<Void> deleteProduct(@PathVariable UUID id) {
		return productService.deleteProduct(id);
	}

	@PatchMapping("/{id}/deactivate")
	@Operation(summary = "Deactivate a product")
	public Mono<ProductResponse> deactivateProduct(@PathVariable UUID id) {
		return productService.deactivateProduct(id)
				.map(DtoMapper::map);
	}

	@PatchMapping("/{id}/activate")
	@Operation(summary = "Activate a product")
	public Mono<ProductResponse> activateProduct(@PathVariable UUID id) {
		return productService.activateProduct(id)
				.map(DtoMapper::map);
	}
}
