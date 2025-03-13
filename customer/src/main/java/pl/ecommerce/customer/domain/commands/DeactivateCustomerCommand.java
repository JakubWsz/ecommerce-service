package pl.ecommerce.customer.domain.commands;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DeactivateCustomerCommand(UUID customerId, String reason) implements Command {
}
