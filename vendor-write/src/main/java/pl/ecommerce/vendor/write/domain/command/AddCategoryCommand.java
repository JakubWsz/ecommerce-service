package pl.ecommerce.vendor.write.domain.command;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.tracing.TracingContext;

import java.util.UUID;

@Builder
public record AddCategoryCommand(
		UUID vendorId,
		UUID categoryId,
		String categoryName,
		TracingContext tracingContext
) implements Command {
}