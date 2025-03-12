package pl.ecommerce.product.api.dto;

import lombok.Builder;
import pl.ecommerce.product.domain.model.Product;

import java.time.LocalDateTime;

@Builder
public record ProductImageResponse(
		String id,
		Product product,
		String url,
		Integer sortOrder,
		LocalDateTime createdAt
) {
}
