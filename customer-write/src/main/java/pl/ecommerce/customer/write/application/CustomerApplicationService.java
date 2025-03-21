package pl.ecommerce.customer.write.application;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.*;
import pl.ecommerce.customer.write.infrastructure.exception.CustomerAlreadyExistsException;
import pl.ecommerce.customer.write.infrastructure.exception.CustomerNotFoundException;
import pl.ecommerce.customer.write.infrastructure.exception.GdprConsentRequiredException;
import pl.ecommerce.customer.write.infrastructure.repository.CustomerRepository;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerApplicationService {

	private final CustomerRepository customerRepository;
	private final ObservationRegistry observationRegistry;

	public Mono<UUID> registerCustomer(RegisterCustomerCommand command) {
		String traceId = getTraceId(command.tracingContext());
		log.info("Registering new customer with email: {}, traceId: {}", command.email(), traceId);

		return Observation.createNotStarted("customer.register", observationRegistry)
				.observe(() -> processRegistration(command, traceId));
	}

	public Mono<UUID> updateCustomer(UpdateCustomerCommand command) {
		UUID customerId = command.customerId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Updating customer with ID: {}, traceId: {}", customerId, traceId);

		return modifyCustomer(customerId, "customer.update",
				"Customer updated successfully: {}, traceId: {}", traceId,
				customer -> customer.updateBasicInfo(command));
	}

	public Mono<UUID> changeEmail(ChangeCustomerEmailCommand command) {
		UUID customerId = command.customerId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Changing email for customer with ID: {}, traceId: {}", customerId, traceId);

		return modifyCustomer(customerId, "customer.changeEmail",
				"Customer email changed successfully: {}, traceId: {}", traceId,
				customer -> customer.changeEmail(command));
	}

	public Mono<UUID> verifyEmail(VerifyCustomerEmailCommand command) {
		UUID customerId = command.customerId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Verifying email for customer with ID: {}, traceId: {}", customerId, traceId);

		return modifyCustomer(customerId, "customer.verifyEmail",
				"Customer email verified successfully: {}, traceId: {}", traceId,
				customer -> customer.verifyEmail(command));
	}

	public Mono<UUID> verifyPhoneNumber(VerifyCustomerPhoneCommand command) {
		UUID customerId = command.customerId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Verifying phone number for customer with ID: {}, traceId: {}", customerId, traceId);

		return modifyCustomer(customerId, "customer.verifyPhoneNumber",
				"Customer phone verified successfully: {}, traceId: {}", traceId,
				customer -> customer.verifyPhoneNumber(command));
	}

	public Mono<UUID> addShippingAddress(AddShippingAddressCommand command) {
		UUID customerId = command.customerId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Adding shipping address for customer with ID: {}, traceId: {}", customerId, traceId);

		return modifyCustomer(customerId, "customer.addShippingAddress",
				"Customer shipping address added successfully: {}, traceId: {}", traceId,
				customer -> customer.addShippingAddress(command));
	}

	public Mono<UUID> updateShippingAddress(UpdateShippingAddressCommand command) {
		UUID customerId = command.customerId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Updating shipping address for customer with ID: {}, traceId: {}", customerId, traceId);

		return modifyCustomer(customerId, "customer.updateShippingAddress",
				"Customer shipping address updated successfully: {}, traceId: {}", traceId,
				customer -> customer.updateShippingAddress(command));
	}

	public Mono<UUID> removeShippingAddress(RemoveShippingAddressCommand command) {
		UUID customerId = command.customerId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Removing shipping address for customer with ID: {}, traceId: {}", customerId, traceId);

		return modifyCustomer(customerId, "customer.removeShippingAddress",
				"Customer shipping address removed successfully: {}, traceId: {}", traceId,
				customer -> customer.removeShippingAddress(command));
	}

	public Mono<UUID> updatePreferences(UpdateCustomerPreferencesCommand command) {
		UUID customerId = command.customerId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Updating preferences for customer with ID: {}, traceId: {}", customerId, traceId);

		return modifyCustomer(customerId, "customer.updatePreferences",
				"Customer preferences updated successfully: {}, traceId: {}", traceId,
				customer -> customer.updatePreferences(command));
	}

	public Mono<UUID> deactivate(DeactivateCustomerCommand command) {
		UUID customerId = command.customerId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Deactivating customer with ID: {}, traceId: {}", customerId, traceId);

		return modifyCustomer(customerId, "customer.deactivate",
				"Customer deactivated successfully: {}, traceId: {}", traceId,
				customer -> customer.deactivate(command));
	}

	public Mono<UUID> reactivate(ReactivateCustomerCommand command) {
		UUID customerId = command.customerId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Reactivating customer with ID: {}, traceId: {}", customerId, traceId);

		return modifyCustomer(customerId, "customer.reactivate",
				"Customer reactivated successfully: {}, traceId: {}", traceId,
				customer -> customer.reactivate(command));
	}

	public Mono<UUID> deleteCustomer(UUID customerId, TracingContext tracingContext) {
		String traceId = getTraceId(tracingContext);
		log.info("Deleting customer with ID: {}, traceId: {}", customerId, traceId);

		return modifyCustomer(customerId, "customer.delete",
				"Customer marked as deleted: {}, traceId: {}",
				traceId,
				customer -> customer.delete(DeleteCustomerCommand.builder()
						.customerId(customerId)
						.reason("User requested deletion")
						.tracingContext(tracingContext)
						.build()));
	}

	private Mono<UUID> processRegistration(RegisterCustomerCommand command, String traceId) {
		return customerRepository.existsByEmail(command.email())
				.flatMap(exists -> {
					if (exists) {
						log.warn("Customer with email {} already exists, traceId: {}", command.email(), traceId);
						return Mono.error(new CustomerAlreadyExistsException("Customer with email already exists", traceId));
					}
					if (command.consents() == null || !command.consents().isGdprConsent()) {
						return Mono.error(new GdprConsentRequiredException("GDPR consent is required for registration", traceId));
					}
					UUID customerId = command.customerId() != null ? command.customerId() : UUID.randomUUID();
					CustomerAggregate customer = new CustomerAggregate(command);
					return customerRepository.save(customer)
							.doOnSuccess(savedCustomer -> log.info("Customer registered successfully: {}, traceId: {}", customerId, traceId))
							.map(CustomerAggregate::getId);
				});
	}

	private Mono<CustomerAggregate> loadCustomerAggregate(UUID customerId) {
		return customerRepository.findById(customerId)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)));
	}

	private Mono<UUID> modifyCustomer(UUID customerId,
									  String observationName,
									  String successMessage,
									  String traceId,
									  Consumer<CustomerAggregate> updater) {
		return Objects.requireNonNull(Observation.createNotStarted(observationName, observationRegistry)
						.observe(() -> loadCustomerAggregate(customerId)
								.flatMap(customer -> {
									updater.accept(customer);
									return customerRepository.save(customer)
											.doOnSuccess(savedCustomer -> log.info(successMessage, customerId, traceId));
								})))
				.map(CustomerAggregate::getId);
	}

	private String getTraceId(TracingContext tracingContext) {
		return tracingContext != null ? tracingContext.getTraceId() : "unknown";
	}
}
