package pl.ecommerce.customer.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.customer.CustomerEmailVerifiedEvent;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.VerifyCustomerEmailCommand;

import java.time.Instant;

public class VerifyCustomerEmailCommandHandler implements CommandHandler<VerifyCustomerEmailCommand> {
	private final CustomerAggregate aggregate;
	private final CustomerAggregate.AggregateHelper helper;

	public VerifyCustomerEmailCommandHandler(CustomerAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(VerifyCustomerEmailCommand command) {
		helper.assertCustomerActive();

		if (aggregate.isEmailVerified()) {
			return;
		}

		helper.applyChange(new CustomerEmailVerifiedEvent(
				aggregate.getId(),
				aggregate.getEmail(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}