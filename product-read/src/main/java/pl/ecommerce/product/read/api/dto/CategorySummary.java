package pl.ecommerce.product.read.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummary {
	private UUID id;
	private String name;
	private String slug;
	private String imageUrl;
	private UUID parentCategoryId;
	private int productCount;
	private int childCount;
	private boolean active;
	private int displayOrder;
	private String traceId;
}