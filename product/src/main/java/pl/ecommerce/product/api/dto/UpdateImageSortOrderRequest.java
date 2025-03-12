package pl.ecommerce.product.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateImageSortOrderRequest(
		@NotNull(message = "New sort order must be provided")
		Integer newSortOrder
) {
}
