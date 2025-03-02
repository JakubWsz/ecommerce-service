package pl.ecommerce.customer.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.customer.domain.model.*;
import pl.ecommerce.customer.domain.repository.CustomerRepository;
import pl.ecommerce.customer.infrastructure.client.GeolocationClient;
import pl.ecommerce.customer.infrastructure.exception.CustomerAlreadyExistsException;
import pl.ecommerce.customer.infrastructure.exception.CustomerNotFoundException;
import pl.ecommerce.customer.infrastructure.exception.GdprConsentRequiredException;
import pl.ecommerce.customer.infrastructure.monitoring.MonitoringService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final GeolocationClient geolocationClient;
	private final MonitoringService monitoringService;

	@Value("${gdpr.data-retention-period-days:730}")
	private Integer dataRetentionPeriodDays;

	@Transactional
	public Mono<Customer> registerCustomer(Customer customer, String ipAddress) {
		log.info("Registering new customer with email: {}", customer.getPersonalData().getEmail());

		if (!customer.isGdprConsent()) {
			return Mono.error(GdprConsentRequiredException.throwEx("GDPR consent is required"));
		}

		return monitoringService.measureDatabaseOperation(() ->
				customerRepository.getCustomerByEmail(customer.getPersonalData().getEmail())
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
									.doOnSuccess(c -> {
										monitoringService.incrementCustomerCreated();
										log.info("Customer registered successfully with id: {}", c.getId());
									});
						}))
		);
	}

	public Mono<Customer> getCustomerById(UUID id) {
		log.debug("Fetching customer with id: {}", id);
		return monitoringService.measureDatabaseOperation(() ->
				customerRepository.getCustomerById(id)
						.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + id)))
						.doOnSuccess(customer -> log.debug("Customer found: {}", customer.getId()))
		);
	}

	public Flux<Customer> getAllCustomers() {
		log.debug("Fetching all active customers");
		return monitoringService.measureDatabaseOperation(() ->
				customerRepository.getAllActiveCustomers()
						.collectList()
		).flatMapMany(Flux::fromIterable);
	}

	@Transactional
	public Mono<Customer> updateCustomer(UUID id, Customer customerUpdate) {
		log.info("Updating customer with id: {}", id);
		return monitoringService.measureDatabaseOperation(() ->
				customerRepository.getCustomerById(id)
						.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + id)))
						.flatMap(existingCustomer -> {
							if (customerUpdate.getPersonalData() != null) {
								existingCustomer.setPersonalData(customerUpdate.getPersonalData());
							}

							if (customerUpdate.isGdprConsent() != existingCustomer.isGdprConsent()) {
								existingCustomer.setGdprConsent(customerUpdate.isGdprConsent());
								existingCustomer.setConsentTimestamp(LocalDateTime.now());
							}
							if (customerUpdate.isMarketingConsent() != existingCustomer.isMarketingConsent()) {
								existingCustomer.setMarketingConsent(customerUpdate.isMarketingConsent());
							}
							if (customerUpdate.isDataProcessingConsent() != existingCustomer.isDataProcessingConsent()) {
								existingCustomer.setDataProcessingConsent(customerUpdate.isDataProcessingConsent());
							}

							if (customerUpdate.getAddresses() != null) {
								existingCustomer.setAddresses(customerUpdate.getAddresses());
							}

							existingCustomer.setUpdatedAt(LocalDateTime.now());

							return customerRepository.updateCustomer(id, existingCustomer)
									.doOnSuccess(c -> {
										monitoringService.incrementCustomerUpdated();
										log.info("Customer updated successfully: {}", c.getId());
									});
						})
		);
	}

	@Transactional
	public Mono<Void> deleteCustomer(UUID id) {
		log.info("Deleting (deactivating) customer with id: {}", id);
		return monitoringService.measureDatabaseOperation(() ->
				customerRepository.getCustomerById(id)
						.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + id)))
						.flatMap(customer -> {
							customer.setActive(false);
							customer.setUpdatedAt(LocalDateTime.now());
							return customerRepository.updateCustomer(id, customer);
						})
						.doOnSuccess(c -> {
							monitoringService.incrementCustomerDeleted();
							log.info("Customer deactivated successfully: {}", id);
						})
						.then()
		);
	}
}

//	public Mono<Map<String, Object>> exportCustomerData(UUID id) {
//		log.info("Exporting customer data for GDPR purposes, id: {}", id);
//		return getCustomerById(id)
//				.flatMap(customer -> {
//					Mono<Map<String, Object>> personalDataMono = Mono.justOrEmpty(
//							customer.getPersonalData() != null ? customer.getPersonalData().toExportFormat() : Map.of());
//					Mono<Map<String, Object>> geoDataMono = Mono.justOrEmpty(
//							customer.getGeoLocationData() != null ? customer.getGeoLocationData().toExportFormat() : Map.of());
//					Mono<List<Object>> addressesMono = customer.getAddresses() != null
//							? Flux.fromIterable(customer.getAddresses())
//							.map(Address::toExportFormat)
//							.collectList()
//							: Mono.just(List.of());
//
//					return Mono.zip(personalDataMono, geoDataMono, addressesMono)
//							.map(tuple -> Map.of(
//									"customerId", customer.getId().toString(),
//									"personalInformation", tuple.getT1(),
//									"geoLocationData", tuple.getT2(),
//									"addresses", tuple.getT3(),
//									"registrationDate", customer.getCreatedAt().toString(),
//									"lastUpdateDate", customer.getUpdatedAt().toString(),
//									"consents", Map.of(
//											"gdprConsent", customer.isGdprConsent(),
//											"marketingConsent", customer.isMarketingConsent(),
//											"dataProcessingConsent", customer.isDataProcessingConsent(),
//											"consentTimestamp", customer.getConsentTimestamp() != null
//													? customer.getConsentTimestamp().toString() : null
//									),
//									"retentionPolicy", Map.of(
//											"retentionPeriodDays", customer.getDataRetentionPeriodDays()
//									)
//							));
//				});
//	}
//
//	@Transactional
//	public Mono<Void> anonymizeCustomer(UUID id) {
//		log.info("Anonymizing customer data (GDPR), id: {}", id);
//		return monitoringService.measureDatabaseOperation(() ->
//				customerRepository.findById(id)
//						.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with id: " + id)))
//						.flatMap(customer -> {
//							customer.anonymize();
//							customer.setUpdatedAt(LocalDateTime.now());
//							return customerRepository.save(customer)
//									.flatMap(savedCustomer -> savedCustomer.getPersonalData() != null
//											? personalDataRepository.save(savedCustomer.getPersonalData()).thenReturn(savedCustomer)
//											: Mono.just(savedCustomer)
//									);
//						})
//						.then()
//		);
//	}
//
//	@Transactional
//	public Mono<Void> requestForgetMe(UUID id) {
//		log.info("Processing 'right to be forgotten' request, id: {}", id);
//		return monitoringService.measureDatabaseOperation(() ->
//				getCustomerById(id)
//						.flatMap(customer -> {
//							customer.anonymize();
//							customer.setUpdatedAt(LocalDateTime.now());
//							customer.setDataRetentionPeriodDays(30);
//							return customerRepository.save(customer)
//									.flatMap(savedCustomer -> savedCustomer.getPersonalData() != null
//											? personalDataRepository.save(savedCustomer.getPersonalData()).thenReturn(savedCustomer)
//											: Mono.just(savedCustomer)
//									);
//						})
//						.then()
//		);
//	}
//
//	@Transactional
//	public Mono<Integer> anonymizeExpiredCustomerData() {
//		log.info("Running GDPR data retention task");
//		LocalDateTime now = LocalDateTime.now();
//		return customerRepository.findByActive(true, Pageable.unpaged())
//				.filter(customer -> customer.getUpdatedAt().isBefore(now.minusDays(dataRetentionPeriodDays)))
//				.flatMap(customer -> {
//					log.info("Anonymizing expired customer data: {}", customer.getId());
//					customer.anonymize();
//					customer.setUpdatedAt(now);
//					return customerRepository.save(customer)
//							.flatMap(savedCustomer -> savedCustomer.getPersonalData() != null
//									? personalDataRepository.save(savedCustomer.getPersonalData()).thenReturn(savedCustomer)
//									: Mono.just(savedCustomer)
//							);
//				})
//				.count()
//				.map(Long::intValue)
//				.doOnSuccess(count -> log.info("Anonymized {} customer records", count))
//				.doOnError(e -> log.error("Error during data anonymization task", e));
//	}
//}
