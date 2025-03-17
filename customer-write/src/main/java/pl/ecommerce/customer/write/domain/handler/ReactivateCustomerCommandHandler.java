package pl.ecommerce.customer.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.customer.CustomerReactivatedEvent;
import pl.ecommerce.commons.model.customer.CustomerStatus;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.ReactivateCustomerCommand;

import java.time.Instant;

public class ReactivateCustomerCommandHandler implements CommandHandler<ReactivateCustomerCommand> {
	private final CustomerAggregate aggregate;
	private final CustomerAggregate.AggregateHelper helper;

	public ReactivateCustomerCommandHandler(CustomerAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(ReactivateCustomerCommand command) {
		if (CustomerStatus.ACTIVE.equals(aggregate.getStatus())) {
			return;
		}

		helper.applyChange(new CustomerReactivatedEvent(
				aggregate.getId(),
				command.note(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}