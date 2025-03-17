package pl.ecommerce.vendor.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.vendor.VendorVerificationCompletedEvent;
import pl.ecommerce.commons.model.vendor.VendorStatus;
import pl.ecommerce.vendor.write.domain.VendorAggregate;
import pl.ecommerce.vendor.write.domain.command.VerifyVendorCommand;

import java.time.Instant;

public class VerifyVendorCommandHandler implements CommandHandler<VerifyVendorCommand> {
	private final VendorAggregate aggregate;
	private final VendorAggregate.AggregateHelper helper;

	public VerifyVendorCommandHandler(VendorAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(VerifyVendorCommand command) {
		if (aggregate.isVerified()) {
			return;
		}

		helper.applyChange(new VendorVerificationCompletedEvent(
				aggregate.getId(),
				command.verificationId(),
				VendorStatus.APPROVED,
				command.verifiedFields(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}