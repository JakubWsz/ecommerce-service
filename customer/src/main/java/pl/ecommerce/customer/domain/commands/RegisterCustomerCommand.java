package pl.ecommerce.customer.domain.commands;

import lombok.Builder;
import pl.ecommerce.customer.api.dto.CustomerConsents;

import java.util.UUID;

@Builder
public record RegisterCustomerCommand(UUID customerId,
									  String email,
									  String firstName,
									  String lastName,
									  String phoneNumber,
									  String password,
									  CustomerConsents consents
) implements Command {
}