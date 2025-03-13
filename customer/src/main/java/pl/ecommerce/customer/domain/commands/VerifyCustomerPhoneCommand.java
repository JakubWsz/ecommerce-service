package pl.ecommerce.customer.domain.commands;

import java.util.UUID;

public record VerifyCustomerPhoneCommand(UUID customerId,
										 String verificationToken) implements Command {
}
