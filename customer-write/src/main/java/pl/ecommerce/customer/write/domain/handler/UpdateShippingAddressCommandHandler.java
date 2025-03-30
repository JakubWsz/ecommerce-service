package pl.ecommerce.customer.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.customer.CustomerAddressUpdatedEvent;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.UpdateShippingAddressCommand;
import pl.ecommerce.customer.write.infrastructure.exception.AddressNotFoundException;

import java.time.Instant;

public class UpdateShippingAddressCommandHandler implements CommandHandler<UpdateShippingAddressCommand> {
	private final CustomerAggregate aggregate;
	private final CustomerAggregate.AggregateHelper helper;

	public UpdateShippingAddressCommandHandler(CustomerAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(UpdateShippingAddressCommand command) {
		helper.assertCustomerActive();

		boolean addressExists = aggregate.getShippingAddresses().stream()
				.anyMatch(address -> address.getId().equals(command.addressId()));

		if (!addressExists) {
			throw new AddressNotFoundException(command.addressId());
		}

		helper.applyChange(new CustomerAddressUpdatedEvent(
				aggregate.getId(),
				command.addressId(),
				command.buildingNumber(),
				command.apartmentNumber(),
				command.street(),
				command.city(),
				command.postalCode(),
				command.country(),
				command.voivodeship(),
				command.isDefault(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}