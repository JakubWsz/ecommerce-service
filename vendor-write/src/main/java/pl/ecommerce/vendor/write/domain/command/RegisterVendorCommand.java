package pl.ecommerce.vendor.write.domain.command;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.tracing.TracingContext;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Builder
public record RegisterVendorCommand(
		UUID vendorId,
		String name,
		String businessName,
		String taxId,
		String email,
		String phone,
		String legalForm,
		Set<UUID> initialCategories,
		BigDecimal commissionRate,
		TracingContext tracingContext
) implements Command {
	@Override
	public UUID getId() {
		return vendorId;
	}
}