package pl.ecommerce.customer.write.domain.commands;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.model.customer.AddressType;

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
										String voivodeship,
										boolean isDefault) implements Command {
	@Override
	public UUID getId() {
		return customerId;
	}
}
