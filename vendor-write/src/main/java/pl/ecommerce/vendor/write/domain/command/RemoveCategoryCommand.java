package pl.ecommerce.vendor.write.domain.command;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.tracing.TracingContext;

import java.util.UUID;

@Builder
public record RemoveCategoryCommand(
		UUID vendorId,
		UUID categoryId,
		TracingContext tracingContext
) implements Command {
	@Override
	public UUID getId() {
		return vendorId;
	}
}