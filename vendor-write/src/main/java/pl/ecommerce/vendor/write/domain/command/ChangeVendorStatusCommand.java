package pl.ecommerce.vendor.write.domain.command;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.model.vendor.VendorStatus;
import pl.ecommerce.commons.tracing.TracingContext;

import java.util.UUID;

@Builder
public record ChangeVendorStatusCommand(
		UUID vendorId,
		VendorStatus newStatus,
		String reason,
		TracingContext tracingContext
) implements Command {
}