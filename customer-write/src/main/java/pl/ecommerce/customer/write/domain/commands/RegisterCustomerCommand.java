package pl.ecommerce.customer.write.domain.commands;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.model.customer.CustomerConsents;

import java.util.UUID;

@Builder
public record RegisterCustomerCommand(UUID customerId,
									  String email,
									  String firstName,
									  String lastName,
									  String phoneNumber,
									  String password,
									  CustomerConsents consents
									 ) implements Command {
	@Override
	public UUID getId() {
		return customerId;
	}
}