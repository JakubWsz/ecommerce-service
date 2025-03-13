package pl.ecommerce.customer.domain.commands;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateShippingAddressCommand(UUID customerId,
										   UUID addressId,
										   String buildingNumber,
										   String apartmentNumber,
										   String street,
										   String city,
										   String postalCode,
										   String country,
										   String state,
										   boolean isDefault
) implements Command {
}
