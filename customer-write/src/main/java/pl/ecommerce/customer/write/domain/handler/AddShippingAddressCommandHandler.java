package pl.ecommerce.customer.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.customer.CustomerAddressAddedEvent;
import pl.ecommerce.commons.model.customer.AddressType;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.AddShippingAddressCommand;

import java.time.Instant;
import java.util.UUID;

public class AddShippingAddressCommandHandler implements CommandHandler<AddShippingAddressCommand> {
	private final CustomerAggregate aggregate;
	private final CustomerAggregate.AggregateHelper helper;

	public AddShippingAddressCommandHandler(CustomerAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(AddShippingAddressCommand command) {
		helper.assertCustomerActive();

		UUID addressId = command.addressId() != null ? command.addressId() : UUID.randomUUID();

		helper.applyChange(new CustomerAddressAddedEvent(
				aggregate.getId(),
				addressId,
				command.addressType(),
				command.buildingNumber(),
				command.apartmentNumber(),
				command.street(),
				command.city(),
				command.postalCode(),
				command.country(),
				command.state(),
				command.isDefault(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}