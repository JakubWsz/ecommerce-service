package pl.ecommerce.customer.write.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.*;
import pl.ecommerce.customer.write.infrastructure.exception.CustomerAlreadyExistsException;
import pl.ecommerce.customer.write.infrastructure.exception.CustomerNotFoundException;
import pl.ecommerce.customer.write.infrastructure.exception.GdprConsentRequiredException;
import pl.ecommerce.customer.write.infrastructure.repository.CustomerRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerApplicationService {

	private final CustomerRepository customerRepository;

	public Mono<CustomerAggregate> registerCustomer(RegisterCustomerCommand command) {
		return customerRepository.existsByEmail(command.email())
				.flatMap(exists -> {
					if (exists) {
						return Mono.error(new CustomerAlreadyExistsException("Customer with email already exists", command.email()));
					}
					if (isNull(command.consents()) || !command.consents().isGdprConsent()) {
						return Mono.error(new GdprConsentRequiredException("GDPR consent is required for registration"));
					}
					UUID customerId = nonNull(command.customerId()) ? command.customerId() : UUID.randomUUID();
					CustomerAggregate customer = new CustomerAggregate(command);

					return customerRepository.save(customer)
							.doOnSuccess(savedCustomer ->
									log.info("Customer registered successfully: {},", customerId));
				});
	}

	public Mono<Void> updateCustomer(UpdateCustomerCommand command) {
		UUID customerId = command.customerId();
		log.info("Updating customer with ID: {}", customerId);
		return modifyCustomer(customerId,
				"Customer updated successfully: {}",
				customer -> customer.updateBasicInfo(command))
				.then();
	}

	public Mono<Void> changeEmail(ChangeCustomerEmailCommand command) {
		UUID customerId = command.customerId();
		log.info("Changing email for customer with ID: {}", customerId);
		return modifyCustomer(customerId,
				"Customer email changed successfully: {}",
				customer -> customer.changeEmail(command))
				.then();
	}

	public Mono<Void> verifyEmail(VerifyCustomerEmailCommand command) {
		UUID customerId = command.customerId();
		log.info("Verifying email for customer with ID: {}", customerId);
		return modifyCustomer(customerId,
				"Customer email verified successfully: {}",
				customer -> customer.verifyEmail(command))
				.then();
	}

	public Mono<Void> verifyPhoneNumber(VerifyCustomerPhoneCommand command) {
		UUID customerId = command.customerId();
		log.info("Verifying phone number for customer with ID: {}", customerId);
		return modifyCustomer(customerId,
				"Customer phone verified successfully: {}",
				customer -> customer.verifyPhoneNumber(command))
				.then();
	}

	public Mono<CustomerAggregate> addShippingAddress(AddShippingAddressCommand command) {
		UUID customerId = command.customerId();
		log.info("Adding shipping address for customer with ID: {}", customerId);
		return modifyCustomer(customerId,
				"Customer shipping address added successfully: {}",
				customer -> customer.addShippingAddress(command));
	}

	public Mono<CustomerAggregate> updateShippingAddress(UpdateShippingAddressCommand command) {
		UUID customerId = command.customerId();
		log.info("Updating shipping address for customer with ID: {}", customerId);
		return modifyCustomer(customerId,
				"Customer shipping address updated successfully: {}",
				customer -> customer.updateShippingAddress(command));
	}

	public Mono<Void> removeShippingAddress(RemoveShippingAddressCommand command) {
		UUID customerId = command.customerId();
		log.info("Removing shipping address for customer with ID: {}", customerId);
		return modifyCustomer(customerId,
				"Customer shipping address removed successfully: {}",
				customer -> customer.removeShippingAddress(command))
				.then();
	}

	public Mono<Void> updatePreferences(UpdateCustomerPreferencesCommand command) {
		UUID customerId = command.customerId();
		log.info("Updating preferences for customer with ID: {}", customerId);
		return modifyCustomer(customerId,
				"Customer preferences updated successfully: {}",
				customer -> customer.updatePreferences(command))
				.then();
	}

	public Mono<Void> deactivate(DeactivateCustomerCommand command) {
		UUID customerId = command.customerId();
		log.info("Deactivating customer with ID: {}", customerId);
		return modifyCustomer(customerId,
				"Customer deactivated successfully: {}",
				customer -> customer.deactivate(command))
				.then();
	}

	public Mono<Void> reactivate(ReactivateCustomerCommand command) {
		UUID customerId = command.getId();
		log.info("Reactivating customer with ID: {}", customerId);
		return modifyCustomer(command.getId(),
				"Customer reactivated successfully: {}",
				customer -> customer.reactivate(command))
				.then();
	}

	public Mono<Void> deleteCustomer(UUID customerId) {
		return Mono.deferContextual(contextView -> modifyCustomer(customerId,
						"Customer marked as deleted: {}",
						customer -> customer.delete(DeleteCustomerCommand.builder()
								.customerId(customerId)
								.reason("User requested deletion")
								.build())))
				.then();
	}

	private Mono<CustomerAggregate> loadCustomerAggregate(UUID customerId) {
		return customerRepository.findById(customerId)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)));
	}

	private Mono<CustomerAggregate> modifyCustomer(UUID customerId,
												   String successMessage,
												   Consumer<CustomerAggregate> updater) {
		return loadCustomerAggregate(customerId)
				.flatMap(customer -> {
					updater.accept(customer);
					return customerRepository.save(customer)
							.doOnSuccess(savedCustomer -> log.info(successMessage, customerId));
				});
	}
}
