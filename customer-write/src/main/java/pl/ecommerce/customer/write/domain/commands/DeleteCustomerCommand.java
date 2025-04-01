package pl.ecommerce.customer.write.domain.commands;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;

import java.util.UUID;

@Builder
public record DeleteCustomerCommand(UUID customerId,
									String reason) implements Command {
	@Override
	public UUID getId() {
		return customerId;
	}
}
