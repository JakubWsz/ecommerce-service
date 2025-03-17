package pl.ecommerce.vendor.write.domain.handler;

import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.vendor.VendorCategoryRemovedEvent;
import pl.ecommerce.vendor.write.domain.VendorAggregate;
import pl.ecommerce.vendor.write.domain.command.RemoveCategoryCommand;
import pl.ecommerce.vendor.write.infrastructure.exception.CategoryNotFoundException;

import java.time.Instant;

public class RemoveCategoryCommandHandler implements CommandHandler<RemoveCategoryCommand> {
	private final VendorAggregate aggregate;
	private final VendorAggregate.AggregateHelper helper;

	public RemoveCategoryCommandHandler(VendorAggregate aggregate) {
		this.aggregate = aggregate;
		this.helper = aggregate.getHelper();
	}

	@Override
	public void handle(RemoveCategoryCommand command) {
		if (!aggregate.getAssignedCategories().contains(command.categoryId())) {
			throw new CategoryNotFoundException(command.categoryId());
		}

		helper.applyChange(new VendorCategoryRemovedEvent(
				aggregate.getId(),
				command.categoryId(),
				Instant.now(),
				aggregate.getVersion()
		));
	}
}