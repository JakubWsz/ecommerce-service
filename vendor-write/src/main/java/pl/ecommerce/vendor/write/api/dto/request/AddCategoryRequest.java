package pl.ecommerce.vendor.write.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to add a category to a vendor")
public class AddCategoryRequest {

	@NotNull(message = "Category ID is required")
	@Schema(description = "Unique identifier of the category", example = "f7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a", required = true)
	private UUID categoryId;

	@Schema(description = "Name of the category", example = "Electronics")
	private String categoryName;
}