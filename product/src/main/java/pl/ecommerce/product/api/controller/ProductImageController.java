package pl.ecommerce.product.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.ecommerce.product.api.DtoMapper;
import pl.ecommerce.product.api.dto.CreateProductImageRequest;
import pl.ecommerce.product.api.dto.ProductImageResponse;
import pl.ecommerce.product.api.dto.UpdateImageSortOrderRequest;
import pl.ecommerce.product.domain.service.ProductImageService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Images", description = "Product Image Management API")
public class ProductImageController {

	private final ProductImageService productImageService;

	@GetMapping("/{productId}/images")
	@Operation(summary = "Get images for a product")
	public Flux<ProductImageResponse> getImagesByProductId(@PathVariable UUID productId) {
		return productImageService.getImagesByProductId(productId)
				.map(DtoMapper::map);
	}

	@PostMapping("/{productId}/images")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Add a new image to a product")
	public Mono<ProductImageResponse> addProductImage(
			@PathVariable UUID productId,
			@Valid @RequestBody CreateProductImageRequest request) {
		return productImageService.addProductImage(productId, request.imageUrl(), request.sortOrder())
				.map(DtoMapper::map);
	}

	@DeleteMapping("/images/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete an image by ID")
	public Mono<Void> deleteProductImage(@PathVariable UUID id) {
		return productImageService.deleteProductImage(id);
	}

	@DeleteMapping("/{productId}/images")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete all images for a product")
	public Mono<Void> deleteAllProductImages(@PathVariable UUID productId) {
		return productImageService.deleteAllProductImages(productId);
	}

	@PatchMapping("/images/{id}/sort-order")
	@Operation(summary = "Update image sort order")
	public Mono<ProductImageResponse> updateImageSortOrder(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateImageSortOrderRequest request) {
		return productImageService.updateImageSortOrder(id, request.newSortOrder())
				.map(DtoMapper::map);
	}
}
