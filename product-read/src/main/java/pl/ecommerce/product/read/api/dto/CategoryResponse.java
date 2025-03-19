package pl.ecommerce.product.read.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
	private UUID id;
	private String name;
	private String description;
	private String slug;
	private UUID parentCategoryId;
	private String parentCategoryName;
	private Set<UUID> subcategoryIds;
	private List<String> subcategoryNames;
	private Map<String, String> attributes;
	private boolean active;
	private String iconUrl;
	private String imageUrl;
	private int displayOrder;
	private int productCount;
	private Instant createdAt;
	private Instant updatedAt;
	private String traceId;
	private List<CategorySummary> children;
}


