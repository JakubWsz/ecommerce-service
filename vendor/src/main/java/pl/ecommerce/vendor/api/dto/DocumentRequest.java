package pl.ecommerce.vendor.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record DocumentRequest(
		@NotBlank(message = "Document type is required")
		String documentType,
		@NotBlank(message = "Document URL is required")
		String documentUrl
) {
}
