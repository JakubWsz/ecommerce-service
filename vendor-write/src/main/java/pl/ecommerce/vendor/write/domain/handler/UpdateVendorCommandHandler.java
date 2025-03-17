package pl.ecommerce.vendor.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.vendor.VendorUpdatedEvent;
import pl.ecommerce.vendor.write.domain.VendorAggregate;
import pl.ecommerce.vendor.write.domain.command.UpdateVendorCommand;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class UpdateVendorCommandHandler implements CommandHandler<UpdateVendorCommand> {
	private final VendorAggregate aggregate;
	private final VendorAggregate.AggregateHelper helper;

	public UpdateVendorCommandHandler(VendorAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(UpdateVendorCommand command) {
		helper.assertVendorActive();

		Map<String, Object> changes = new HashMap<>();
		boolean hasChanges = false;

		if (command.name() != null && !command.name().equals(aggregate.getName())) {
			changes.put("name", command.name());
			hasChanges = true;
		}

		if (command.businessName() != null && !command.businessName().equals(aggregate.getBusinessName())) {
			changes.put("businessName", command.businessName());
			hasChanges = true;
		}

		if (command.phone() != null && !command.phone().equals(aggregate.getPhone())) {
			changes.put("phone", command.phone());
			hasChanges = true;
		}

		if (command.contactPersonName() != null && !command.contactPersonName().equals(aggregate.getContactPersonName())) {
			changes.put("contactPersonName", command.contactPersonName());
			hasChanges = true;
		}

		if (command.contactPersonEmail() != null && !command.contactPersonEmail().equals(aggregate.getContactPersonEmail())) {
			changes.put("contactPersonEmail", command.contactPersonEmail());
			hasChanges = true;
		}

		if (hasChanges) {
			helper.applyChange(new VendorUpdatedEvent(
					aggregate.getId(),
					changes,
					Instant.now(),
					aggregate.getVersion()
			));
		}
	}
}