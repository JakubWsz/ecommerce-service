package pl.ecommerce.customer.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.customer.CustomerEmailChangedEvent;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.ChangeCustomerEmailCommand;
import pl.ecommerce.customer.write.infrastructure.exception.InvalidCustomerDataException;

import java.time.Instant;

public class ChangeCustomerEmailCommandHandler implements CommandHandler<ChangeCustomerEmailCommand> {
	private final CustomerAggregate aggregate;
	private final CustomerAggregate.AggregateHelper helper;

	public ChangeCustomerEmailCommandHandler(CustomerAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(ChangeCustomerEmailCommand command) {
		helper.assertCustomerActive();

		if (command.newEmail() == null || command.newEmail().isBlank()) {
			throw new InvalidCustomerDataException("New email cannot be empty");
		}

		if (command.newEmail().equals(aggregate.getEmail())) {
			return;
		}

		helper.applyChange(new CustomerEmailChangedEvent(
				aggregate.getId(),
				aggregate.getEmail(),
				command.newEmail(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}