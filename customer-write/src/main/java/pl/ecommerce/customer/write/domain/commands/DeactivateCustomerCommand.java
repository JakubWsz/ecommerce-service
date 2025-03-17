package pl.ecommerce.customer.write.domain.commands;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.tracing.TracingContext;

import java.util.UUID;

@Builder
public record DeactivateCustomerCommand(UUID customerId,
										String reason,
										TracingContext tracingContext) implements Command {
}
