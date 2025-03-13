package pl.ecommerce.customer.domain.commands;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateCustomerCommand(UUID customerId,
									String firstName,
									String lastName,
									String phoneNumber
) implements Command {
}