package pl.ecommerce.customer.domain.commands;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ChangeCustomerEmailCommand(UUID customerId,
										 String newEmail) implements Command {
}
