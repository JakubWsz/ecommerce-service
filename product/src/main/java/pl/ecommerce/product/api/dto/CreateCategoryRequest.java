package pl.ecommerce.product.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCategoryRequest(
		@NotBlank String name,
		String description,
		UUID parentId
) {
}