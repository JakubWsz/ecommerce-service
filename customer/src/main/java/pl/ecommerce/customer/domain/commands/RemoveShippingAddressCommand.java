package pl.ecommerce.customer.domain.commands;

import lombok.Builder;

import java.util.UUID;

@Builder
public record RemoveShippingAddressCommand(UUID customerId,
										   UUID addressId) implements Command {
}
