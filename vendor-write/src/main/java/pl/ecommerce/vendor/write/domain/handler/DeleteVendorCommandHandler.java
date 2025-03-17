package pl.ecommerce.vendor.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.vendor.VendorDeletedEvent;
import pl.ecommerce.vendor.write.domain.VendorAggregate;
import pl.ecommerce.vendor.write.domain.command.DeleteVendorCommand;

import java.time.Instant;

public class DeleteVendorCommandHandler implements CommandHandler<DeleteVendorCommand> {
	private final VendorAggregate aggregate;
	private final VendorAggregate.AggregateHelper helper;

	public DeleteVendorCommandHandler(VendorAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(DeleteVendorCommand command) {
		helper.applyChange(new VendorDeletedEvent(
				aggregate.getId(),
				command.reason(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}