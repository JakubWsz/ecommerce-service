package pl.ecommerce.vendor.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.vendor.VendorCategoryAssignedEvent;
import pl.ecommerce.vendor.write.domain.VendorAggregate;
import pl.ecommerce.vendor.write.domain.command.AddCategoryCommand;
import pl.ecommerce.vendor.write.infrastructure.exception.CategoryAlreadyAssignedException;

import java.time.Instant;

public class AddCategoryCommandHandler implements CommandHandler<AddCategoryCommand> {
	private final VendorAggregate aggregate;
	private final VendorAggregate.AggregateHelper helper;

	public AddCategoryCommandHandler(VendorAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(AddCategoryCommand command) {
		if (aggregate.getAssignedCategories().contains(command.categoryId())) {
			throw new CategoryAlreadyAssignedException(command.categoryId());
		}

		helper.applyChange(new VendorCategoryAssignedEvent(
				aggregate.getId(),
				command.categoryId(),
				command.categoryName(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}