package pl.ecommerce.vendor.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.vendor.VendorBankDetailsUpdatedEvent;
import pl.ecommerce.vendor.write.domain.VendorAggregate;
import pl.ecommerce.vendor.write.domain.command.UpdateBankDetailsCommand;

import java.time.Instant;

public class UpdateBankDetailsCommandHandler implements CommandHandler<UpdateBankDetailsCommand> {
	private final VendorAggregate aggregate;
	private final VendorAggregate.AggregateHelper helper;

	public UpdateBankDetailsCommandHandler(VendorAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(UpdateBankDetailsCommand command) {
		helper.applyChange(new VendorBankDetailsUpdatedEvent(
				aggregate.getId(),
				command.bankAccountNumber(),
				command.bankName(),
				command.bankSwiftCode(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}