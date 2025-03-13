package pl.ecommerce.customer.api.dto;

import lombok.Builder;

@Builder
public record ErrorResponse(
		 String message,
		 String code,
		 String timestamp,
		 String details
) {
}
