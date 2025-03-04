package pl.ecommerce.customer.infrastructure.utils;

import pl.ecommerce.commons.event.customer.CustomerDeletedEvent;
import pl.ecommerce.commons.event.customer.CustomerRegisteredEvent;
import pl.ecommerce.commons.event.customer.CustomerUpdatedEvent;
import pl.ecommerce.customer.domain.model.Customer;

import java.util.Map;
import java.util.UUID;

public final class CustomerEventUtils {

	private CustomerEventUtils() {
	}

	public static CustomerRegisteredEvent createCustomerRegisteredEvent(Customer customer) {
		return CustomerRegisteredEvent.builder()
				.correlationId(UUID.randomUUID())
				.customerId(customer.getId())
				.email(customer.getPersonalData().getEmail())
				.firstName(customer.getPersonalData().getFirstName())
				.lastName(customer.getPersonalData().getLastName())
				.build();
	}

	public static CustomerUpdatedEvent createCustomerUpdatedEvent(Customer updatedCustomer, Map<String, Object> changes) {
		return CustomerUpdatedEvent.builder()
				.correlationId(UUID.randomUUID())
				.customerId(updatedCustomer.getId())
				.changes(changes)
				.build();
	}

	public static CustomerDeletedEvent createCustomerDeletedEvent(Customer customer) {
		return CustomerDeletedEvent.builder()
				.correlationId(UUID.randomUUID())
				.customerId(customer.getId())
				.email(customer.getPersonalData().getEmail())
				.firstName(customer.getPersonalData().getFirstName())
				.lastName(customer.getPersonalData().getLastName())
				.build();
	}
}