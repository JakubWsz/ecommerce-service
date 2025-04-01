package pl.ecommerce.customer.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.customer.CustomerRegisteredEvent;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.RegisterCustomerCommand;
import pl.ecommerce.customer.write.infrastructure.exception.InvalidCustomerDataException;

import java.time.Instant;

public class RegisterCustomerCommandHandler implements CommandHandler<RegisterCustomerCommand> {
	private final CustomerAggregate.AggregateHelper helper;

	public RegisterCustomerCommandHandler(CustomerAggregate aggregate) {
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(RegisterCustomerCommand command) {
		if (command.email() == null || command.email().isBlank()) {
			throw new InvalidCustomerDataException("Email is required");
		}
		if (command.firstName() == null || command.firstName().isBlank()) {
			throw new InvalidCustomerDataException("First name is required");
		}
		if (command.lastName() == null || command.lastName().isBlank()) {
			throw new InvalidCustomerDataException("Last name is required");
		}

		helper.applyChange(new CustomerRegisteredEvent(
				command.customerId(),
				command.email(),
				command.firstName(),
				command.lastName(),
				command.phoneNumber(),
				Instant.now(),
				0
		));
	}
}