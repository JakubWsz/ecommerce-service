package pl.ecommerce.customer.aplication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.customer.domain.exceptions.CustomerAlreadyExistsException;
import pl.ecommerce.customer.domain.exceptions.CustomerNotFoundException;
import pl.ecommerce.customer.domain.exceptions.GdprConsentRequiredException;
import reactor.core.publisher.Mono;
import pl.ecommerce.customer.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.domain.commands.*;
import pl.ecommerce.customer.infrastructure.repository.CustomerRepository;
import pl.ecommerce.customer.infrastructure.client.GeolocationClient;

import java.util.UUID;

/**
 * Serwis aplikacyjny do obsługi operacji na kliencie.
 * Stanowi warstwę pośrednią między kontrolerami API a logiką domenową.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerApplicationService {

	private final CustomerRepository customerRepository;
	private final CustomerQueryService customerQueryService;
	private final GeolocationClient geolocationClient;

	@Value("${gdpr.data-retention-period-days:730}")
	private Integer dataRetentionPeriodDays;

	/**
	 * Rejestruje nowego klienta
	 *
	 * @param request   Komenda rejestracji
	 * @param ipAddress Adres IP klienta
	 * @return Mono z identyfikatorem utworzonego klienta
	 */
	@Transactional
	public Mono<UUID> registerCustomer(RegisterCustomerCommand request, String ipAddress) {
		log.info("Registering new customer with email: {}", request.email());

		// Sprawdź, czy klient o podanym emailu już istnieje
		return customerRepository.existsByEmail(request.email())
				.flatMap(exists -> {
					if (exists) {
						log.warn("Customer with email {} already exists", request.email());
						return Mono.error(new CustomerAlreadyExistsException("Customer with email already exists"));
					}

					// Walidacja zgody GDPR
					if (request.consents() == null || !request.consents().gdprConsent()) {
						return Mono.error(new GdprConsentRequiredException("GDPR consent is required for registration"));
					}

					UUID customerId = request.customerId() != null ? request.customerId() : UUID.randomUUID();

					CustomerAggregate customer = new CustomerAggregate(RegisterCustomerCommand.builder()
							.customerId(customerId)
							.email(request.email())
							.firstName(request.firstName())
							.lastName(request.lastName())
							.phoneNumber(request.phoneNumber())
							.build());

					return enrichWithGeolocation(customer, ipAddress)
							.flatMap(customerRepository::save)
							.map(CustomerAggregate::getId);
				});
	}

	/**
	 * Aktualizuje dane klienta
	 *
	 * @param customerId ID klienta
	 * @param command    Komenda aktualizacji
	 * @return Mono z ID klienta
	 */
	@Transactional
	public Mono<UUID> updateCustomer(UUID customerId, UpdateCustomerCommand command) {
		log.info("Updating customer with ID: {}", customerId);

		return customerRepository.findById(customerId)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)))
				.flatMap(customer -> {
					// Aktualizacja danych klienta
					customer.updateBasicInfo(UpdateCustomerCommand.builder()
							.customerId(customerId)
							.firstName(command.firstName())
							.lastName(command.lastName())
							.phoneNumber(command.phoneNumber())
							.build());

					return customerRepository.save(customer)
							.map(CustomerAggregate::getId);
				});
	}

	/**
	 * Dodaje adres do klienta
	 *
	 * @param customerId ID klienta
	 * @param command    Komenda dodania adresu
	 * @return Mono z ID klienta
	 */
	@Transactional
	public Mono<UUID> addAddress(UUID customerId, AddShippingAddressCommand command) {
		log.info("Adding address for customer with ID: {}", customerId);

		return customerRepository.findById(customerId)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)))
				.flatMap(customer -> {
					// Dodanie adresu do klienta
					customer.addShippingAddress(AddShippingAddressCommand.builder()
							.customerId(customerId)
							.addressId(UUID.randomUUID())
							.addressType(command.addressType())
							.apartmentNumber(command.apartmentNumber())
							.buildingNumber(command.buildingNumber())
							.street(command.street())
							.city(command.city())
							.postalCode(command.postalCode())
							.country(command.country())
							.state(command.state())
							.isDefault(command.isDefault())
							.build());

					return customerRepository.save(customer)
							.map(CustomerAggregate::getId);
				});
	}

	/**
	 * Usuwa adres klienta
	 *
	 * @param customerId ID klienta
	 * @param addressId  ID adresu
	 * @return Mono z ID klienta
	 */
	@Transactional
	public Mono<UUID> removeAddress(UUID customerId, UUID addressId) {
		log.info("Removing address {} for customer with ID: {}", addressId, customerId);

		return customerRepository.findById(customerId)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)))
				.flatMap(customer -> {
					// Usunięcie adresu z klienta
					customer.removeShippingAddress(RemoveShippingAddressCommand.builder()
							.customerId(customerId)
							.addressId(addressId)
							.build());

					return customerRepository.save(customer)
							.map(CustomerAggregate::getId);
				});
	}

	/**
	 * Aktualizuje preferencje klienta
	 *
	 * @param customerId ID klienta
	 * @param command    Komenda aktualizacji preferencji
	 * @return Mono z ID klienta
	 */
	@Transactional
	public Mono<UUID> updatePreferences(UUID customerId, UpdateCustomerPreferencesCommand command) {
		log.info("Updating preferences for customer with ID: {}", customerId);

		return customerRepository.findById(customerId)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)))
				.flatMap(customer -> {
					customer.updatePreferences(command);

					return customerRepository.save(customer)
							.map(CustomerAggregate::getId);
				});
	}

	/**
	 * Zmienia email klienta
	 *
	 * @param customerId ID klienta
	 * @param command    Komenda zmiany emaila
	 * @return Mono z ID klienta
	 */
	@Transactional
	public Mono<UUID> changeEmail(UUID customerId, ChangeCustomerEmailCommand command) {
		log.info("Changing email for customer with ID: {}", customerId);

		return customerRepository.findById(customerId)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)))
				.flatMap(customer -> {
					customer.changeEmail(command);

					return customerRepository.save(customer)
							.map(CustomerAggregate::getId);
				});
	}

	/**
	 * Weryfikuje email klienta
	 *
	 * @param customerId ID klienta
	 * @param token      Token weryfikacyjny
	 * @return Mono z ID klienta
	 */
	@Transactional
	public Mono<UUID> verifyEmail(UUID customerId, String token) {
		log.info("Verifying email for customer with ID: {}", customerId);

		return customerRepository.findById(customerId)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)))
				.flatMap(customer -> {
					customer.verifyEmail(VerifyCustomerEmailCommand.builder()
							.customerId(customerId)
							.verificationToken(token)
							.build());

					return customerRepository.save(customer)
							.map(CustomerAggregate::getId);
				});
	}

	/**
	 * Dezaktywuje klienta
	 *
	 * @param customerId ID klienta
	 * @param reason     Powód dezaktywacji
	 * @return Mono z ID klienta
	 */
	@Transactional
	public Mono<UUID> deactivateCustomer(UUID customerId, String reason) {
		log.info("Deactivating customer with ID: {}", customerId);

		return customerRepository.findById(customerId)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)))
				.flatMap(customer -> {
					customer.deactivate(DeactivateCustomerCommand.builder()
							.customerId(customerId)
							.reason(reason)
							.build());

					return customerRepository.save(customer)
							.map(CustomerAggregate::getId);
				});
	}

	/**
	 * Usuwa klienta
	 *
	 * @param customerId ID klienta
	 * @return Mono z ID klienta
	 */
	@Transactional
	public Mono<UUID> deleteCustomer(UUID customerId) {
		log.info("Deleting customer with ID: {}", customerId);

		return customerRepository.findById(customerId)
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)))
				.flatMap(customer -> {
					customer.delete(DeleteCustomerCommand.builder()
							.customerId(customerId)
							.reason("User requested deletion")
							.build());

					return customerRepository.save(customer)
							.map(CustomerAggregate::getId);
				});
	}

	/**
	 * Wzbogaca klienta o dane geolokalizacyjne na podstawie IP
	 *
	 * @param customer  Agregat klienta
	 * @param ipAddress Adres IP
	 * @return Mono z agregatem klienta
	 */
	private Mono<CustomerAggregate> enrichWithGeolocation(CustomerAggregate customer, String ipAddress) {
		if (ipAddress == null || ipAddress.isEmpty()) {
			return Mono.just(customer);
		}

		return geolocationClient.getLocationByIp(ipAddress)
				.map(geoData -> {
					// Tu moglibyśmy dodać nową metodę do agregatu, która obsługuje dane geolokalizacyjne,
					// ale dla uproszczenia zostawiamy to jako oddzielny etap
					return customer;
				})
				.onErrorResume(e -> {
					log.warn("Failed to get geolocation data: {}", e.getMessage());
					return Mono.just(customer);
				});
	}
}