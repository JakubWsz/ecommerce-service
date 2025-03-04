package pl.ecommerce.customer.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.customer.domain.model.*;
import pl.ecommerce.customer.domain.repository.CustomerRepository;
import pl.ecommerce.customer.infrastructure.client.GeolocationClient;
import pl.ecommerce.customer.infrastructure.exception.CustomerAlreadyExistsException;
import pl.ecommerce.customer.infrastructure.exception.CustomerNotFoundException;
import pl.ecommerce.customer.infrastructure.exception.GdprConsentRequiredException;
import pl.ecommerce.customer.infrastructure.exception.InternalAppException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static pl.ecommerce.customer.infrastructure.utils.CustomerEventUtils.*;
import static pl.ecommerce.customer.infrastructure.utils.CustomerInitializationUtils.*;
import static pl.ecommerce.customer.infrastructure.utils.CustomerMessagesUtils.*;
import static pl.ecommerce.customer.infrastructure.utils.CustomerUpdateUtils.updateConsentField;
import static pl.ecommerce.customer.infrastructure.utils.CustomerUpdateUtils.updateFieldIfPresent;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final GeolocationClient geolocationClient;
	private final EventPublisher eventPublisher;

	@Value("${gdpr.data-retention-period-days:730}")
	private Integer dataRetentionPeriodDays;

	@Transactional
	public Mono<Customer> registerCustomer(Customer customer, String ipAddress) {
		log.info(LOG_REGISTERING_CUSTOMER, customer.getPersonalData().getEmail());

		if (!customer.isGdprConsent()) {
			return Mono.error(new GdprConsentRequiredException());
		}

		return checkIfCustomerExists(customer.getPersonalData().getEmail())
				.then(Mono.defer(() -> Mono.just(initializeNewCustomer(customer, ipAddress, dataRetentionPeriodDays))))
				.flatMap(this::fetchGeoLocationIfNeeded)
				.flatMap(customerRepository::saveCustomer)
				.switchIfEmpty(Mono.error(new InternalAppException(ERROR_FAILED_TO_SAVE_CUSTOMER)))
				.doOnSuccess(this::publishCustomerRegisteredEvent)
				.doOnError(e -> log.error(LOG_ERROR_SAVING_CUSTOMER, e.getMessage(), e))
				.onErrorMap(e -> new InternalAppException(ERROR_FAILED_TO_REGISTER_CUSTOMER + e.getMessage(), e));
	}

	public Mono<Customer> getCustomerById(UUID id) {
		log.debug(LOG_FETCHING_CUSTOMER, id);

		return customerRepository.getCustomerById(id)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException(ERROR_CUSTOMER_NOT_FOUND + id)))
				.doOnNext(c -> log.debug(LOG_CUSTOMER_FOUND, c))
				.doOnError(e -> log.error(ERROR_FETCHING_CUSTOMER, e));
	}

	public Flux<Customer> getAllCustomers() {
		log.debug(LOG_FETCHING_ALL_CUSTOMERS);

		return customerRepository.getAllActiveCustomers();
	}

	@Transactional
	public Mono<Customer> updateCustomer(UUID id, Customer customerUpdate) {
		log.info(LOG_UPDATING_CUSTOMER, id);

		return customerRepository.getCustomerById(id)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException(ERROR_CUSTOMER_NOT_FOUND + id)))
				.flatMap(existingCustomer -> updateCustomerData(id, customerUpdate, existingCustomer));
	}

	@Transactional
	public Mono<Void> deleteCustomer(UUID id) {
		log.info(LOG_DELETING_CUSTOMER, id);

		return customerRepository.getCustomerById(id)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException(ERROR_CUSTOMER_NOT_FOUND + id)))
				.flatMap(this::deactivateCustomer)
				.doOnSuccess(this::publishCustomerDeletedEvent)
				.then();
	}

	private Mono<Customer> deactivateCustomer(Customer customer) {
		var deactivatedCustomer = prepareForDeactivation(customer);
		return customerRepository.updateCustomer(customer.getId(), deactivatedCustomer);
	}

	private Mono<Void> checkIfCustomerExists(String email) {
		return customerRepository.getCustomerByEmail(email)
				.flatMap(existing -> {
					log.warn(LOG_CUSTOMER_EXISTS, email);
					return Mono.error(new CustomerAlreadyExistsException());
				})
				.then();
	}

	private Mono<Customer> fetchGeoLocationIfNeeded(Customer customer) {
		if (customer.getRegistrationIp() == null || customer.getRegistrationIp().isEmpty()) {
			return Mono.just(customer);
		}

		return geolocationClient.getLocationByIp(customer.getRegistrationIp())
				.flatMap(geoResponse -> Mono.just(mapGeoLocationDataToCustomer(customer, geoResponse)))
				.onErrorResume(e -> {
					log.warn(LOG_GEOLOCATION_ERROR, e.getMessage());
					return Mono.just(customer);
				});
	}

	private Mono<Customer> updateCustomerData(UUID id, Customer customerUpdate, Customer existingCustomer) {
		Map<String, Object> changes = new HashMap<>();

		updateFieldIfPresent(customerUpdate.getPersonalData(),
				existingCustomer::setPersonalData,
				"personalData",
				changes);

		updateConsentField(customerUpdate.isGdprConsent(),
				existingCustomer.isGdprConsent(),
				existingCustomer::setGdprConsent,
				"gdprConsent",
				changes,
				() -> existingCustomer.setConsentTimestamp(LocalDateTime.now()));

		updateConsentField(customerUpdate.isMarketingConsent(),
				existingCustomer.isMarketingConsent(),
				existingCustomer::setMarketingConsent,
				"marketingConsent",
				changes);

		updateConsentField(customerUpdate.isDataProcessingConsent(),
				existingCustomer.isDataProcessingConsent(),
				existingCustomer::setDataProcessingConsent,
				"dataProcessingConsent",
				changes);

		updateFieldIfPresent(customerUpdate.getAddresses(),
				existingCustomer::setAddresses,
				"addresses",
				changes);

		LocalDateTime now = LocalDateTime.now();
		existingCustomer.setUpdatedAt(now);
		changes.put("updatedAt", now);

		return customerRepository.updateCustomer(id, existingCustomer)
				.doOnSuccess(updatedCustomer -> publishCustomerUpdatedEvent(updatedCustomer, changes));
	}

	private void publishCustomerRegisteredEvent(Customer customer) {
		log.info(LOG_CUSTOMER_REGISTERED, customer.getId());

		var customerRegisteredEvent = createCustomerRegisteredEvent(customer);
		eventPublisher.publish(customerRegisteredEvent);
	}

	private void publishCustomerUpdatedEvent(Customer updatedCustomer, Map<String, Object> changes) {
		log.info(LOG_CUSTOMER_UPDATED, updatedCustomer.getId());

		var event = createCustomerUpdatedEvent(updatedCustomer, changes);
		eventPublisher.publish(event);
	}

	private void publishCustomerDeletedEvent(Customer customer) {
		log.info(LOG_CUSTOMER_DEACTIVATED, customer.getId());

		var customerDeletedEvent = createCustomerDeletedEvent(customer);
		eventPublisher.publish(customerDeletedEvent);
	}
}
