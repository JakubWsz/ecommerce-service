package pl.ecommerce.customer.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.customer.CustomerPhoneVerifiedEvent;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.VerifyCustomerPhoneCommand;

import java.time.Instant;

public class VerifyCustomerPhoneCommandHandler implements CommandHandler<VerifyCustomerPhoneCommand> {
	private final CustomerAggregate aggregate;
	private final CustomerAggregate.AggregateHelper helper;

	public VerifyCustomerPhoneCommandHandler(CustomerAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(VerifyCustomerPhoneCommand command) {
		helper.assertCustomerActive();

		if (aggregate.isPhoneVerified() || aggregate.getPhoneNumber() == null) {
			return;
		}

		helper.applyChange(new CustomerPhoneVerifiedEvent(
				aggregate.getId(),
				aggregate.getPhoneNumber().value(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}