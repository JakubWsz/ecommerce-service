package pl.ecommerce.vendor.write.domain.command;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.tracing.TracingContext;

import java.util.UUID;

@Builder
public record UpdateVendorCommand(
		UUID vendorId,
		String name,
		String businessName,
		String phone,
		String contactPersonName,
		String contactPersonEmail,
		TracingContext tracingContext
) implements Command {
	@Override
	public UUID getId() {
		return vendorId;
	}
}