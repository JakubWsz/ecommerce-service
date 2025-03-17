package pl.ecommerce.customer.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.customer.CustomerDeactivatedEvent;
import pl.ecommerce.commons.model.customer.CustomerStatus;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.DeactivateCustomerCommand;

import java.time.Instant;

public class DeactivateCustomerCommandHandler implements CommandHandler<DeactivateCustomerCommand> {
	private final CustomerAggregate aggregate;
	private final CustomerAggregate.AggregateHelper helper;

	public DeactivateCustomerCommandHandler(CustomerAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(DeactivateCustomerCommand command) {
		if (CustomerStatus.INACTIVE.equals(aggregate.getStatus())) {
			return;
		}

		helper.applyChange(new CustomerDeactivatedEvent(
				aggregate.getId(),
				command.reason(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}