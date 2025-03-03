package pl.ecommerce.customer.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.event.customer.CustomerDeletedEvent;
import pl.ecommerce.commons.event.customer.CustomerRegisteredEvent;
import pl.ecommerce.commons.event.customer.CustomerUpdatedEvent;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.customer.domain.model.*;
import pl.ecommerce.customer.domain.repository.CustomerRepository;
import pl.ecommerce.customer.infrastructure.client.GeolocationClient;
import pl.ecommerce.customer.infrastructure.exception.CustomerAlreadyExistsException;
import pl.ecommerce.customer.infrastructure.exception.CustomerNotFoundException;
import pl.ecommerce.customer.infrastructure.exception.GdprConsentRequiredException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
		log.info("Registering new customer with email: {}", customer.getPersonalData().getEmail());

		if (!customer.isGdprConsent()) {
			return Mono.error(GdprConsentRequiredException.throwEx("GDPR consent is required"));
		}

		return customerRepository.getCustomerByEmail(customer.getPersonalData().getEmail())
				.flatMap(existing -> {
					log.warn("Customer with email {} already exists", customer.getPersonalData().getEmail());
					return Mono.error(CustomerAlreadyExistsException.throwEx("Customer with this email already exists"));
				})
				.then(Mono.defer(() -> {
					LocalDateTime now = LocalDateTime.now();
					customer.setActive(true);
					customer.setRegistrationIp(ipAddress);
					customer.setCreatedAt(now);
					customer.setUpdatedAt(now);
					customer.setConsentTimestamp(now);
					customer.setDataRetentionPeriodDays(dataRetentionPeriodDays);

					Mono<Customer> customerMono = Mono.just(customer);

					if (ipAddress != null && !ipAddress.isEmpty()) {
						customerMono = geolocationClient.getLocationByIp(ipAddress)
								.flatMap(geoResponse -> {
									GeoLocationData geoData = new GeoLocationData(
											geoResponse.getCountry(),
											geoResponse.getCity(),
											geoResponse.getState(),
											geoResponse.getPostalCode()
									);
									customer.setGeoLocationData(geoData);

									if (geoResponse.getCurrency() != null && !geoResponse.getCurrency().isEmpty()) {
										List<MonetaryAmount> currencies = geoResponse.getCurrency().entrySet().stream()
												.map(entry -> (MonetaryAmount) Money.of(new BigDecimal(entry.getValue()), Monetary.getCurrency(entry.getKey())))
												.toList();
										customer.setCurrencies(currencies);
									}
									return Mono.just(customer);
								})
								.onErrorResume(e -> {
									log.warn("Could not fetch geolocation data: {}", e.getMessage());
									return Mono.just(customer);
								});
					}

					return customerMono
							.flatMap(customerRepository::saveCustomer)
							.switchIfEmpty(Mono.error(new RuntimeException("Failed to save customer")))
							.doOnSuccess(c -> {
								log.info("Customer registered successfully with id: {}", c.getId());
								var customerRegisteredEvent = CustomerRegisteredEvent.builder()
										.correlationId(UUID.randomUUID())
										.customerId(c.getId())
										.email(c.getPersonalData().getEmail())
										.firstName(c.getPersonalData().getFirstName())
										.lastName(c.getPersonalData().getLastName())
										.build();
								eventPublisher.publish(customerRegisteredEvent);
							})
							.doOnError(e -> log.error("Error occurred while saving customer: {}", e.getMessage(), e))
							.onErrorMap(e -> new RuntimeException("Custom error message: Failed to register customer. Reason: " + e.getMessage(), e)); // Przekazanie błędu dalej

				}));
	}

	public Mono<Customer> getCustomerById(UUID id) {
		log.debug("Fetching customer with id: {}", id);
		return customerRepository.getCustomerById(id)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + id)))
				.doOnNext(c -> log.debug("Customer found: {}", c))
				.doOnError(e -> log.error("Error fetching customer", e));
	}

	public Flux<Customer> getAllCustomers() {
		log.debug("Fetching all active customers");
		return customerRepository.getAllActiveCustomers();
	}

	@Transactional
	public Mono<Customer> updateCustomer(UUID id, Customer customerUpdate) {
		log.info("Updating customer with id: {}", id);

		return customerRepository.getCustomerById(id)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + id)))
				.flatMap(existingCustomer -> {
					Map<String, Object> changes = new HashMap<>();

					if (customerUpdate.getPersonalData() != null) {
						existingCustomer.setPersonalData(customerUpdate.getPersonalData());
						changes.put("personalData", customerUpdate.getPersonalData());
					}

					if (customerUpdate.isGdprConsent() != existingCustomer.isGdprConsent()) {
						existingCustomer.setGdprConsent(customerUpdate.isGdprConsent());
						existingCustomer.setConsentTimestamp(LocalDateTime.now());
						changes.put("gdprConsent", customerUpdate.isGdprConsent());
					}

					if (customerUpdate.isMarketingConsent() != existingCustomer.isMarketingConsent()) {
						existingCustomer.setMarketingConsent(customerUpdate.isMarketingConsent());
						changes.put("marketingConsent", customerUpdate.isMarketingConsent());
					}

					if (customerUpdate.isDataProcessingConsent() != existingCustomer.isDataProcessingConsent()) {
						existingCustomer.setDataProcessingConsent(customerUpdate.isDataProcessingConsent());
						changes.put("dataProcessingConsent", customerUpdate.isDataProcessingConsent());
					}

					if (customerUpdate.getAddresses() != null) {
						existingCustomer.setAddresses(customerUpdate.getAddresses());
						changes.put("addresses", customerUpdate.getAddresses());
					}

					existingCustomer.setUpdatedAt(LocalDateTime.now());
					changes.put("updatedAt", existingCustomer.getUpdatedAt());

					return customerRepository.updateCustomer(id, existingCustomer)
							.doOnSuccess(updatedCustomer -> {
								log.info("Customer updated successfully: {}", updatedCustomer.getId());

								CustomerUpdatedEvent event = CustomerUpdatedEvent.builder()
										.correlationId(UUID.randomUUID())
										.customerId(updatedCustomer.getId())
										.changes(changes)
										.build();

								eventPublisher.publish(event);
							});
				});
	}

	@Transactional
	public Mono<Void> deleteCustomer(UUID id) {
		log.info("Deleting (deactivating) customer with id: {}", id);
		return customerRepository.getCustomerById(id)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + id)))
				.flatMap(customer -> {
					customer.setActive(false);
					customer.setUpdatedAt(LocalDateTime.now());
					return customerRepository.updateCustomer(id, customer);
				})
				.doOnSuccess(c -> {
					log.info("Customer deactivated successfully: {}", id);
					var customerDeletedEvent = CustomerDeletedEvent.builder()
							.correlationId(UUID.randomUUID())
							.customerId(c.getId())
							.email(c.getPersonalData().getEmail())
							.firstName(c.getPersonalData().getFirstName())
							.lastName(c.getPersonalData().getLastName())
							.build();
					eventPublisher.publish(customerDeletedEvent);
				})
				.then();
	}
}
