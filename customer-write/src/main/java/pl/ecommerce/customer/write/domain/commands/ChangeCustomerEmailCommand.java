package pl.ecommerce.customer.write.domain.commands;

import lombok.Builder;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.tracing.TracingContext;

import java.util.UUID;

@Builder
public record ChangeCustomerEmailCommand(UUID customerId,
										 String newEmail,
										 TracingContext tracingContext) implements Command {
}
