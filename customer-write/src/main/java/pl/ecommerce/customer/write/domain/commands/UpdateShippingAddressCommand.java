package pl.ecommerce.customer.write.domain.commands;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.tracing.TracingContext;

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
										   boolean isDefault,
										   TracingContext tracingContext) implements Command {
}
