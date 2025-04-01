package pl.ecommerce.vendor.write.domain.command;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.tracing.TracingContext;

import java.util.List;
import java.util.UUID;

@Builder
public record VerifyVendorCommand(
		UUID vendorId,
		UUID verificationId,
		List<String> verifiedFields,
		TracingContext tracingContext
) implements Command {
	@Override
	public UUID getId() {
		return vendorId;
	}
}