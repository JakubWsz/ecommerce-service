package pl.ecommerce.customer.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.customer.CustomerPreferencesUpdatedEvent;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.UpdateCustomerPreferencesCommand;

import java.time.Instant;

public class UpdateCustomerPreferencesCommandHandler implements CommandHandler<UpdateCustomerPreferencesCommand> {
	private final CustomerAggregate aggregate;
	private final CustomerAggregate.AggregateHelper helper;

	public UpdateCustomerPreferencesCommandHandler(CustomerAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(UpdateCustomerPreferencesCommand command) {
		helper.assertCustomerActive();

		helper.applyChange(new CustomerPreferencesUpdatedEvent(
				aggregate.getId(),
				command.preferences(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}