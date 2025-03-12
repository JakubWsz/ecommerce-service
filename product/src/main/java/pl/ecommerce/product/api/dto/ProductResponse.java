package pl.ecommerce.product.api.dto;

import lombok.Builder;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Builder
public record ProductResponse(
		UUID id,
		String name,
		String description,
		MonetaryAmount price,
		int stock,
		boolean active,
		Set<CategoryResponse> categorises,
		Map<String, String> attributes,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		Set<ProductImageResponse> imageUrls
) {}