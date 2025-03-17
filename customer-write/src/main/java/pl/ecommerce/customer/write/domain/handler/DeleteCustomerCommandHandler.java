package pl.ecommerce.customer.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.customer.CustomerDeletedEvent;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.DeleteCustomerCommand;

import java.time.Instant;

public class DeleteCustomerCommandHandler implements CommandHandler<DeleteCustomerCommand> {
	private final CustomerAggregate aggregate;
	private final CustomerAggregate.AggregateHelper helper;

	public DeleteCustomerCommandHandler(CustomerAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(DeleteCustomerCommand command) {
		helper.applyChange(new CustomerDeletedEvent(
				aggregate.getId(),
				aggregate.getEmail(),
				aggregate.getFirstName(),
				aggregate.getLastName(),
				command.reason(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}