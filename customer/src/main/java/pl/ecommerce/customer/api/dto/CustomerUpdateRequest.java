package pl.ecommerce.customer.api.dto;

import lombok.Builder;

@Builder
public record CustomerUpdateRequest(String firstName,
									String lastName,
									String phoneNumber) {
}
