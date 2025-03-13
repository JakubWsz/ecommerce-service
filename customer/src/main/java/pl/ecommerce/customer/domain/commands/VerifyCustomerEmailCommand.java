package pl.ecommerce.customer.domain.commands;

import lombok.Builder;

import java.util.UUID;

@Builder
public record VerifyCustomerEmailCommand(UUID customerId,
										 String verificationToken
) implements Command {
}
