package pl.ecommerce.customer.write.domain.commands;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.tracing.TracingContext;

import java.util.UUID;

@Builder
public record ReactivateCustomerCommand(UUID customerId,
										String note,
										TracingContext tracingContext) implements Command {
}
