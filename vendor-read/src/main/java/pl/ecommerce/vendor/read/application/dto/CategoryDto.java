package pl.ecommerce.vendor.read.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Category information")
public class CategoryDto {

	@Schema(description = "Unique identifier of the category", example = "e7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a")
	private UUID id;

	@Schema(description = "Name of the category", example = "Electronics")
	private String name;

	@Schema(description = "Timestamp when the category was assigned to the vendor", example = "2023-01-01T12:00:00Z")
	private Instant assignedAt;
}