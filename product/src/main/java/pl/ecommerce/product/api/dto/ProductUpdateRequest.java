package pl.ecommerce.product.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import javax.money.MonetaryAmount;
import java.util.Map;

public record ProductUpdateRequest(
		@NotBlank(message = "Product name is required")
		String name,
		String description,
		@NotNull(message = "Price is required")
		MonetaryAmount price,
		@NotNull(message = "Stock is required")
		@Min(value = 0, message = "Stock cannot be negative")
		Integer stock,
		Integer reservedStock,
		@NotBlank(message = "Vendor ID is required")
		String vendorId,
		boolean active,
		Map<String, String> attributes
) {
}
