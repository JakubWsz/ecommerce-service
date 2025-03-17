package pl.ecommerce.customer.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.customer.CustomerAddressRemovedEvent;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.RemoveShippingAddressCommand;
import pl.ecommerce.customer.write.infrastructure.exception.AddressNotFoundException;
import pl.ecommerce.customer.write.infrastructure.exception.CannotRemoveDefaultAddressException;

import java.time.Instant;

public class RemoveShippingAddressCommandHandler implements CommandHandler<RemoveShippingAddressCommand> {
	private final CustomerAggregate aggregate;
	private final CustomerAggregate.AggregateHelper helper;

	public RemoveShippingAddressCommandHandler(CustomerAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(RemoveShippingAddressCommand command) {
		helper.assertCustomerActive();

		boolean addressExists = aggregate.getShippingAddresses().stream()
				.anyMatch(address -> address.getId().equals(command.addressId()));

		if (!addressExists) {
			throw new AddressNotFoundException(command.addressId());
		}

		if (aggregate.getDefaultShippingAddressId() != null &&
				aggregate.getDefaultShippingAddressId().equals(command.addressId())) {
			throw new CannotRemoveDefaultAddressException(command.addressId());
		}

		helper.applyChange(new CustomerAddressRemovedEvent(
				aggregate.getId(),
				command.addressId(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}