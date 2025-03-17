package pl.ecommerce.vendor.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.vendor.VendorRegisteredEvent;
import pl.ecommerce.commons.model.vendor.VendorStatus;
import pl.ecommerce.vendor.write.domain.VendorAggregate;
import pl.ecommerce.vendor.write.domain.command.RegisterVendorCommand;
import pl.ecommerce.vendor.write.infrastructure.exception.InvalidVendorDataException;

import java.time.Instant;

public class RegisterVendorCommandHandler implements CommandHandler<RegisterVendorCommand> {
	private final VendorAggregate.AggregateHelper helper;

	public RegisterVendorCommandHandler(VendorAggregate aggregate) {
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(RegisterVendorCommand command) {
		if (command.email() == null || command.email().isBlank()) {
			throw new InvalidVendorDataException("Email is required");
		}
		if (command.name() == null || command.name().isBlank()) {
			throw new InvalidVendorDataException("Vendor name is required");
		}
		if (command.businessName() == null || command.businessName().isBlank()) {
			throw new InvalidVendorDataException("Business name is required");
		}

		helper.applyChange(new VendorRegisteredEvent(
				command.vendorId(),
				command.name(),
				command.businessName(),
				command.taxId(),
				command.email(),
				command.phone(),
				command.legalForm(),
				command.initialCategories(),
				command.commissionRate(),
				VendorStatus.PENDING,
				Instant.now(),
				0
		));
	}
}