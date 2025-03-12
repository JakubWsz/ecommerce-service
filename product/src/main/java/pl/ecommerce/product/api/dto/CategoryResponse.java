package pl.ecommerce.product.api.dto;

import lombok.Builder;
import pl.ecommerce.product.domain.model.Category;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record CategoryResponse(
		UUID id,
		String name,
		String description,
		Category parent,
		List<Category> children,
		boolean active,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
