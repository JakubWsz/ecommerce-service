package pl.ecommerce.customer.domain.commands;

import lombok.Builder;
import pl.ecommerce.customer.domain.valueobjects.AddressType;

import java.util.UUID;

@Builder
public record AddShippingAddressCommand(UUID customerId,
										UUID addressId,
										String buildingNumber,
										String apartmentNumber,
										AddressType addressType,
										String street,
										String city,
										String postalCode,
										String country,
										String state,
										boolean isDefault) implements Command {
}
