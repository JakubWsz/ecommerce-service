package pl.ecommerce.product.api.dto;

public record UpdateCategoryRequest(
		String name,
		String description,
		Boolean active
) {
}
