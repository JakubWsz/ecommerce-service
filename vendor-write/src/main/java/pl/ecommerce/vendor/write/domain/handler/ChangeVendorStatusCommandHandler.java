package pl.ecommerce.vendor.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.vendor.VendorStatusChangedEvent;
import pl.ecommerce.vendor.write.domain.VendorAggregate;
import pl.ecommerce.vendor.write.domain.command.ChangeVendorStatusCommand;

import java.time.Instant;

public class ChangeVendorStatusCommandHandler implements CommandHandler<ChangeVendorStatusCommand> {
	private final VendorAggregate aggregate;
	private final VendorAggregate.AggregateHelper helper;

	public ChangeVendorStatusCommandHandler(VendorAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(ChangeVendorStatusCommand command) {
		if (command.newStatus().equals(aggregate.getStatus())) {
			return;
		}

		helper.applyChange(new VendorStatusChangedEvent(
				aggregate.getId(),
				aggregate.getStatus(),
				command.newStatus(),
				command.reason(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}