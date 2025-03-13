package pl.ecommerce.customer.domain.commands;

import lombok.Builder;
import pl.ecommerce.customer.domain.valueobjects.CustomerPreferences;

import java.util.UUID;

@Builder
public record UpdateCustomerPreferencesCommand(UUID customerId,
											   CustomerPreferences preferences) implements Command {
}
