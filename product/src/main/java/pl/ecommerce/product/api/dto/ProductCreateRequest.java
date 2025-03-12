package pl.ecommerce.product.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import javax.money.MonetaryAmount;
import java.util.Map;
import java.util.UUID;

public record ProductCreateRequest(
		@NotBlank(message = "Product name is required")
		String name,
		String description,
		@NotNull(message = "Price is required")
		MonetaryAmount price,
		@NotNull(message = "Stock is required")
		@Min(value = 0, message = "Stock cannot be negative")
		Integer stock,
		Integer reservedStock,
		Map<String, String> attributes,
		@NotNull(message = "Vendor ID is required")
		UUID vendorId
) {

}
