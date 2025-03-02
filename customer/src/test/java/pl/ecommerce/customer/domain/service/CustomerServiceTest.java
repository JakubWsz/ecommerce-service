package pl.ecommerce.customer.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ecommerce.customer.domain.model.Address;
import pl.ecommerce.customer.domain.model.AddressType;
import pl.ecommerce.customer.domain.model.Customer;
import pl.ecommerce.customer.domain.model.PersonalData;
import pl.ecommerce.customer.domain.repository.CustomerRepository;
import pl.ecommerce.customer.infrastructure.client.GeolocationClient;
import pl.ecommerce.customer.infrastructure.monitoring.MonitoringService;
import pl.ecommerce.customer.infrastructure.exception.CustomerAlreadyExistsException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private GeolocationClient geolocationClient;

	@Mock
	private MonitoringService monitoringService;

	@InjectMocks
	private CustomerService customerService;

	private Customer customer;

	@BeforeEach
	public void setup() {
		PersonalData pd = new PersonalData("test@example.com", "Test", "User", "123456789");
		Address address = new Address("Main Street", "10", "A", "City", "State", "12345", "Country", true, AddressType.SHIPPING);

		customer = new Customer(
				true,
				"127.0.0.1",
				LocalDateTime.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				true,
				LocalDateTime.now(),
				false,
				false,
				730,
				pd,
				List.of(address),
				null,
				null
		);
	}

	@Test
	public void testRegisterCustomer_DuplicateEmail_ShouldFail() {
		when(monitoringService.measureDatabaseOperation(any())).thenAnswer(inv -> {
			Supplier<Mono<?>> supplier = inv.getArgument(0);
			return supplier.get();
		});

		when(customerRepository.getCustomerByEmail(any())).thenReturn(Mono.just(customer));

		StepVerifier.create(customerService.registerCustomer(customer, "127.0.0.1"))
				.expectErrorMatches(e -> e instanceof CustomerAlreadyExistsException &&
						e.getMessage().contains("already exists"))
				.verify();
	}

	@Test
	public void testRegisterCustomer_GeolocationError_ShouldFallback() {
		when(monitoringService.measureDatabaseOperation(any())).thenAnswer(inv -> {
			Supplier<Mono<?>> supplier = inv.getArgument(0);
			return supplier.get();
		});

		when(customerRepository.getCustomerByEmail(any())).thenReturn(Mono.empty());
		when(geolocationClient.getLocationByIp(any()))
				.thenReturn(Mono.error(new RuntimeException("Geolocation error"))); // Symulacja błędu
		when(customerRepository.saveCustomer(any(Customer.class))).thenReturn(Mono.just(customer));

		StepVerifier.create(customerService.registerCustomer(customer, "127.0.0.1"))
				.expectNextMatches(c -> c.getPersonalData().getEmail().equals("test@example.com"))
				.verifyComplete();
	}

	@Test
	public void testRegisterCustomer_WithAddresses_ShouldSaveAddresses() {
		when(monitoringService.measureDatabaseOperation(any())).thenAnswer(inv -> {
			Supplier<Mono<?>> supplier = inv.getArgument(0);
			return supplier.get();
		});

		when(customerRepository.getCustomerByEmail(any())).thenReturn(Mono.empty());
		when(customerRepository.saveCustomer(any(Customer.class))).thenReturn(Mono.just(customer));

		StepVerifier.create(customerService.registerCustomer(customer, null))
				.expectNextMatches(c -> c.getAddresses() != null && c.getAddresses().size() == 1)
				.verifyComplete();
	}

	@Test
	public void testUpdateCustomer_ShouldUpdatePersonalData() {
		when(monitoringService.measureDatabaseOperation(any())).thenAnswer(inv -> {
			Supplier<Mono<?>> supplier = inv.getArgument(0);
			return supplier.get();
		});

		PersonalData updatedPd = new PersonalData("updated@example.com", "Updated", "User", "987654321");
		Customer update = new Customer(
				true,
				"127.0.0.1",
				LocalDateTime.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				true,
				LocalDateTime.now(),
				false,
				false,
				730,
				updatedPd,
				null,
				null,
				null
		);

		when(customerRepository.getCustomerById(any(UUID.class))).thenReturn(Mono.just(customer));
		when(customerRepository.updateCustomer(any(UUID.class), any(Customer.class))).thenReturn(Mono.just(update));

		StepVerifier.create(customerService.updateCustomer(customer.getId(), update))
				.expectNextMatches(c -> c.getPersonalData().getEmail().equals("updated@example.com"))
				.verifyComplete();
	}

	@Test
	public void testDeleteCustomer_ShouldDeactivate() {
		when(monitoringService.measureDatabaseOperation(any())).thenAnswer(inv -> {
			Supplier<Mono<?>> supplier = inv.getArgument(0);
			return supplier.get();
		});

		customer.setActive(true);
		when(customerRepository.getCustomerById(any(UUID.class))).thenReturn(Mono.just(customer));
		when(customerRepository.updateCustomer(any(UUID.class), any(Customer.class))).thenReturn(Mono.just(customer));

		StepVerifier.create(customerService.deleteCustomer(customer.getId()))
				.verifyComplete();
	}
}