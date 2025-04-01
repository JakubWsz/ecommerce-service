package pl.ecommerce.product.read.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "categories")
public class CategoryReadModel {
	@Id
	private UUID id;

	private String name;

	private String description;

	@Indexed(unique = true)
	private String slug;

	private UUID parentCategoryId;

	private String parentCategoryName;

	private Set<UUID> subcategoryIds = new HashSet<>();

	private List<String> subcategoryNames = new ArrayList<>();

	private Map<String, String> attributes = new HashMap<>();

	private boolean active;

	private String iconUrl;

	private String imageUrl;

	private int displayOrder;

	private int productCount;

	private Instant createdAt;

	private Instant updatedAt;

	private String lastTraceId;

	private String lastSpanId;

	private String lastOperation;

	private Instant lastUpdatedAt;

	public boolean isRoot() {
		return parentCategoryId == null;
	}

	public boolean hasSubcategories() {
		return !subcategoryIds.isEmpty();
	}
}