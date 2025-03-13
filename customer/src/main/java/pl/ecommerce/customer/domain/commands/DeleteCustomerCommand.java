package pl.ecommerce.customer.domain.commands;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DeleteCustomerCommand(UUID customerId, String reason) implements Command {
}
