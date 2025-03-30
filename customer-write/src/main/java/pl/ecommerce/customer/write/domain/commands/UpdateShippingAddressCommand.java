package pl.ecommerce.customer.write.domain.commands;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;

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
										   String voivodeship,
										   boolean isDefault) implements Command {
	@Override
	public UUID getId() {
		return customerId;
	}
}
