package pl.ecommerce.customer.domain.commands;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ReactivateCustomerCommand(UUID customerId) implements Command {
}
