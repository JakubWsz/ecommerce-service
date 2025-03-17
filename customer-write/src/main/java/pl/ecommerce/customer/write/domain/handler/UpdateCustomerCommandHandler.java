package pl.ecommerce.customer.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.customer.CustomerUpdatedEvent;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.UpdateCustomerCommand;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class UpdateCustomerCommandHandler implements CommandHandler<UpdateCustomerCommand> {
	private final CustomerAggregate aggregate;
	private final CustomerAggregate.AggregateHelper helper;

	public UpdateCustomerCommandHandler(CustomerAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(UpdateCustomerCommand command) {
		helper.assertCustomerActive();

		Map<String, Object> changes = new HashMap<>();
		boolean hasChanges = false;

		if (command.firstName() != null && !command.firstName().equals(aggregate.getFirstName())) {
			changes.put("firstName", command.firstName());
			hasChanges = true;
		}

		if (command.lastName() != null && !command.lastName().equals(aggregate.getLastName())) {
			changes.put("lastName", command.lastName());
			hasChanges = true;
		}

		if (command.phoneNumber() != null &&
				(aggregate.getPhoneNumber() == null || !command.phoneNumber().equals(aggregate.getPhoneNumber().value()))) {
			changes.put("phoneNumber", command.phoneNumber());
			changes.put("phoneVerified", false);
			hasChanges = true;
		}

		if (hasChanges) {
			helper.applyChange(new CustomerUpdatedEvent(
					aggregate.getId(),
					changes,
					Instant.now(),
					aggregate.getVersion()
			));
		}
	}
}