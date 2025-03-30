package pl.ecommerce.customer.write.domain.commands;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.model.customer.CustomerPreferences;

import java.util.UUID;

@Builder
public record UpdateCustomerPreferencesCommand(UUID customerId,
											   CustomerPreferences preferences) implements Command {
	@Override
	public UUID getId() {
		return customerId;
	}
}
