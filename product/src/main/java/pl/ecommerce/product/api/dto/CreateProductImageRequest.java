package pl.ecommerce.product.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProductImageRequest(
		@NotBlank(message = "Image URL cannot be blank")
		String imageUrl,
		Integer sortOrder
) {
}
