package pl.ecommerce.customer.api.dto;

import lombok.Builder;

@Builder
public record AddressRequest(String street,
							 String buildingNumber,
							 String apartmentNumber,
							 String city,
							 String state,
							 String postalCode,
							 String country,
							 boolean isDefault,
							 String addressType
) {
}
