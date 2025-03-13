//package pl.ecommerce.customer.domain.service;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Captor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import pl.ecommerce.commons.kafka.EventPublisher;
//import pl.ecommerce.customer.domain.model.*;
//import pl.ecommerce.customer.domain.repository.CustomerRepositoryMongo;
//import pl.ecommerce.customer.infrastructure.client.GeolocationClient;
//import pl.ecommerce.customer.infrastructure.client.dto.GeoLocationResponse;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class CustomerServiceTest {
//
//	@Mock
//	private CustomerRepositoryMongo customerRepository;
//
//	@Mock
//	private GeolocationClient geolocationClient;
//
//	@Mock
//	private EventPublisher eventPublisher;
//
//	@Captor
//	private ArgumentCaptor<CustomerRegisteredEvent> registeredEventCaptor;
//
//	@Captor
//	private ArgumentCaptor<CustomerUpdatedEvent> updatedEventCaptor;
//
//	@Captor
//	private ArgumentCaptor<CustomerDeletedEvent> deletedEventCaptor;
//
//	@InjectMocks
//	private CustomerService customerService;
//
//	private Customer customer;
//	private UUID customerId;
//	private GeoLocationResponse geoResponse;
//
//	@BeforeEach
//	public void setup() {
//		PersonalData pd = new PersonalData("test@example.com", "Test", "User", "123456789");
//		Address address = new Address("Main Street", "10", "A", "City", "State", "12345", "Country", true, AddressType.SHIPPING);
//		customerId = UUID.randomUUID();
//
//		customer = new Customer(
//				true,
//				"127.0.0.1",
//				LocalDateTime.now(),
//				LocalDateTime.now(),
//				LocalDateTime.now(),
//				true,
//				LocalDateTime.now(),
//				false,
//				false,
//				730,
//				pd,
//				List.of(address),
//				null,
//				null
//		);
//		customer.setId(customerId);
//
//		geoResponse = new GeoLocationResponse();
//		geoResponse.setCountry("Poland");
//		geoResponse.setCity("Warsaw");
//		geoResponse.setState("Mazovia");
//		geoResponse.setPostalCode("00-001");
//
//		Map<String, String> currencyMap = new HashMap<>();
//		currencyMap.put("PLN", "100");
//		geoResponse.setCurrency(currencyMap);
//	}
//
//	@Test
//	public void testRegisterCustomer_DuplicateEmail_ShouldFail() {
//		when(customerRepository.getCustomerByEmail(any())).thenReturn(Mono.just(customer));
//
//		StepVerifier.create(customerService.registerCustomer(customer, "127.0.0.1"))
//				.expectErrorMatches(e -> e.getMessage().contains("Customer with this email already exists"))
//				.verify();
//
//		verify(eventPublisher, never()).publish(any());
//	}
//
//	@Test
//	public void testRegisterCustomer_GeolocationError_ShouldFallback() {
//		when(customerRepository.getCustomerByEmail(any())).thenReturn(Mono.empty());
//		when(geolocationClient.getLocationByIp(any()))
//				.thenReturn(Mono.error(new RuntimeException("Geolocation error")));
//		when(customerRepository.saveCustomer(any(Customer.class))).thenReturn(Mono.just(customer));
//		StepVerifier.create(customerService.registerCustomer(customer, "127.0.0.1"))
//				.expectNextMatches(c -> c.getPersonalData().getEmail().equals("test@example.com"))
//				.verifyComplete();
//
//		verify(eventPublisher).publish(registeredEventCaptor.capture());
//		CustomerRegisteredEvent event = registeredEventCaptor.getValue();
//		assertEquals(customerId, event.getCustomerId());
//		assertEquals("test@example.com", event.getEmail());
//		assertEquals("Test", event.getFirstName());
//		assertEquals("User", event.getLastName());
//	}
//
//	@Test
//	public void testRegisterCustomer_WithAddresses_ShouldSaveAddresses() {
//		when(customerRepository.getCustomerByEmail(any())).thenReturn(Mono.empty());
//		when(customerRepository.saveCustomer(any(Customer.class))).thenReturn(Mono.just(customer));
//
//		StepVerifier.create(customerService.registerCustomer(customer, null))
//				.expectNextMatches(c -> c.getAddresses() != null && c.getAddresses().size() == 1)
//				.verifyComplete();
//
//		verify(eventPublisher).publish(registeredEventCaptor.capture());
//		CustomerRegisteredEvent event = registeredEventCaptor.getValue();
//		assertEquals(customerId, event.getCustomerId());
//	}
//
//	@Test
//	public void testUpdateCustomer_ShouldUpdatePersonalData() {
//		PersonalData updatedPd = new PersonalData("updated@example.com", "Updated", "User", "987654321");
//		Customer update = new Customer(
//				true,
//				"127.0.0.1",
//				LocalDateTime.now(),
//				LocalDateTime.now(),
//				LocalDateTime.now(),
//				true,
//				LocalDateTime.now(),
//				false,
//				false,
//				730,
//				updatedPd,
//				null,
//				null,
//				null
//		);
//		update.setId(UUID.randomUUID());
//
//		when(customerRepository.getCustomerById(any(UUID.class))).thenReturn(Mono.just(customer));
//		when(customerRepository.updateCustomer(any(UUID.class), any(Customer.class))).thenReturn(Mono.just(update));
//
//		StepVerifier.create(customerService.updateCustomer(customer.getId(), update))
//				.expectNextMatches(c -> c.getPersonalData().getEmail().equals("updated@example.com"))
//				.verifyComplete();
//
//		verify(eventPublisher).publish(updatedEventCaptor.capture());
//		CustomerUpdatedEvent event = updatedEventCaptor.getValue();
//		assertEquals(update.getId(), event.getCustomerId());
//		assertNotNull(event.getChanges());
//		assertTrue(event.getChanges().containsKey("personalData"));
//		assertTrue(event.getChanges().containsKey("updatedAt"));
//	}
//
//	@Test
//	public void testDeleteCustomer_ShouldDeactivate() {
//		customer.setActive(true);
//		when(customerRepository.getCustomerById(any(UUID.class))).thenReturn(Mono.just(customer));
//
//		Customer deactivatedCustomer = new Customer(customer);
//		deactivatedCustomer.setActive(false);
//
//		when(customerRepository.updateCustomer(any(UUID.class), any(Customer.class)))
//				.thenReturn(Mono.just(deactivatedCustomer));
//
//		StepVerifier.create(customerService.deleteCustomer(customer.getId()))
//				.verifyComplete();
//
//		verify(eventPublisher).publish(deletedEventCaptor.capture());
//		CustomerDeletedEvent event = deletedEventCaptor.getValue();
//		assertEquals(customerId, event.getCustomerId());
//		assertEquals("test@example.com", event.getEmail());
//		assertEquals("Test", event.getFirstName());
//		assertEquals("User", event.getLastName());
//	}
//
//	@Test
//	public void testRegisterCustomer_Success() {
//		when(customerRepository.getCustomerByEmail(any())).thenReturn(Mono.empty());
//		when(customerRepository.saveCustomer(any(Customer.class))).thenReturn(Mono.just(customer));
//
//		StepVerifier.create(customerService.registerCustomer(customer, null))
//				.expectNextMatches(c -> c.getPersonalData().getEmail().equals("test@example.com"))
//				.verifyComplete();
//
//
//		verify(eventPublisher).publish(any(CustomerRegisteredEvent.class));
//	}
//
//	@Test
//	public void testRegisterCustomer_WithGeolocation_Success() {
//		GeoLocationResponse geoResponse = new GeoLocationResponse();
//		geoResponse.setCountry("Poland");
//		geoResponse.setCity("Warsaw");
//		geoResponse.setState("Mazovia");
//		geoResponse.setPostalCode("00-001");
//
//		Map<String, String> currencyMap = new HashMap<>();
//		currencyMap.put("PLN", "100");
//		geoResponse.setCurrency(currencyMap);
//
//		when(customerRepository.getCustomerByEmail(any())).thenReturn(Mono.empty());
//		when(geolocationClient.getLocationByIp(any())).thenReturn(Mono.just(geoResponse));
//		when(customerRepository.saveCustomer(any(Customer.class))).thenReturn(Mono.just(customer));
//
//		StepVerifier.create(customerService.registerCustomer(customer, "192.168.1.1"))
//				.expectNextMatches(c -> c.getPersonalData().getEmail().equals("test@example.com"))
//				.verifyComplete();
//
//		verify(geolocationClient).getLocationByIp("192.168.1.1");
//
//		verify(eventPublisher).publish(any(CustomerRegisteredEvent.class));
//	}
//
//	@Test
//	public void testGetCustomerById_Found() {
//		when(customerRepository.getCustomerById(customerId)).thenReturn(Mono.just(customer));
//
//		StepVerifier.create(customerService.getCustomerById(customerId))
//				.expectNextMatches(c -> c.getId().equals(customerId))
//				.verifyComplete();
//
//		verify(eventPublisher, never()).publish(any());
//	}
//
//	@Test
//	public void testGetCustomerById_NotFound() {
//		UUID nonExistentId = UUID.randomUUID();
//		when(customerRepository.getCustomerById(nonExistentId)).thenReturn(Mono.empty());
//
//		StepVerifier.create(customerService.getCustomerById(nonExistentId))
//				.expectError(CustomerNotFoundException.class)
//				.verify();
//
//		verify(eventPublisher, never()).publish(any());
//	}
//
//	@Test
//	public void testGetAllCustomers_ReturnsAllActiveCustomers() {
//		Customer customer2 = new Customer(customer);
//		customer2.setId(UUID.randomUUID());
//		customer2.getPersonalData().setEmail("another@example.com");
//
//		when(customerRepository.getAllActiveCustomers()).thenReturn(Flux.just(customer, customer2));
//
//		StepVerifier.create(customerService.getAllCustomers())
//				.expectNextCount(2)
//				.verifyComplete();
//
//		verify(eventPublisher, never()).publish(any());
//	}
//
//	@Test
//	public void testGetAllCustomers_EmptyList() {
//		when(customerRepository.getAllActiveCustomers()).thenReturn(Flux.empty());
//
//		StepVerifier.create(customerService.getAllCustomers())
//				.expectNextCount(0)
//				.verifyComplete();
//
//		verify(eventPublisher, never()).publish(any());
//	}
//
//	@Test
//	public void testUpdateCustomer_CustomerNotFound() {
//		UUID nonExistentId = UUID.randomUUID();
//		when(customerRepository.getCustomerById(nonExistentId)).thenReturn(Mono.empty());
//
//		StepVerifier.create(customerService.updateCustomer(nonExistentId, customer))
//				.expectError(CustomerNotFoundException.class)
//				.verify();
//
//		verify(customerRepository, never()).updateCustomer(any(UUID.class), any(Customer.class));
//
//		verify(eventPublisher, never()).publish(any());
//	}
//
//	@Test
//	public void testUpdateCustomer_ShouldUpdateGdprConsent() {
//		Customer original = new Customer(customer);
//		original.setGdprConsent(false);
//
//		Customer update = new Customer(customer);
//		update.setGdprConsent(true);
//
//		Customer updatedCustomer = new Customer(original);
//		updatedCustomer.setGdprConsent(true);
//		updatedCustomer.setConsentTimestamp(LocalDateTime.now());
//
//		when(customerRepository.getCustomerById(customerId)).thenReturn(Mono.just(original));
//		when(customerRepository.updateCustomer(eq(customerId), any(Customer.class)))
//				.thenReturn(Mono.just(updatedCustomer));
//
//		StepVerifier.create(customerService.updateCustomer(customerId, update))
//				.expectNextMatches(c -> c.isGdprConsent() && c.getConsentTimestamp() != null)
//				.verifyComplete();
//
//		verify(eventPublisher).publish(updatedEventCaptor.capture());
//		CustomerUpdatedEvent event = updatedEventCaptor.getValue();
//		assertEquals(customerId, event.getCustomerId());
//		assertTrue(event.getChanges().containsKey("gdprConsent"));
//		assertEquals(true, event.getChanges().get("gdprConsent"));
//	}
//
//	@Test
//	public void testUpdateCustomer_ShouldUpdateAddresses() {
//		Customer original = new Customer(customer);
//		original.setAddresses(null);
//
//		Address newAddress = new Address("New Street", "20", "B", "NewCity", "NewState",
//				"54321", "NewCountry", true, AddressType.BILLING);
//
//		Customer update = new Customer(customer);
//		update.setAddresses(List.of(newAddress));
//
//		Customer updatedCustomer = new Customer(original);
//		updatedCustomer.setAddresses(List.of(newAddress));
//
//		when(customerRepository.getCustomerById(customerId)).thenReturn(Mono.just(original));
//		when(customerRepository.updateCustomer(eq(customerId), any(Customer.class)))
//				.thenReturn(Mono.just(updatedCustomer));
//
//		StepVerifier.create(customerService.updateCustomer(customerId, update))
//				.expectNextMatches(c ->
//						c.getAddresses() != null &&
//								c.getAddresses().size() == 1 &&
//								c.getAddresses().getFirst().getStreet().equals("New Street")
//				)
//				.verifyComplete();
//
//		verify(eventPublisher).publish(updatedEventCaptor.capture());
//		CustomerUpdatedEvent event = updatedEventCaptor.getValue();
//		assertEquals(customerId, event.getCustomerId());
//		assertTrue(event.getChanges().containsKey("addresses"));
//	}
//
//	@Test
//	public void testDeleteCustomer_CustomerNotFound() {
//		UUID nonExistentId = UUID.randomUUID();
//		when(customerRepository.getCustomerById(nonExistentId)).thenReturn(Mono.empty());
//
//		StepVerifier.create(customerService.deleteCustomer(nonExistentId))
//				.expectError(CustomerNotFoundException.class)
//				.verify();
//
//		verify(customerRepository, never()).updateCustomer(any(UUID.class), any(Customer.class));
//
//		verify(eventPublisher, never()).publish(any());
//	}
//
//	@Test
//	public void testUpdateMultipleConsentSettings() {
//		Customer original = new Customer(customer);
//		original.setMarketingConsent(false);
//		original.setDataProcessingConsent(false);
//
//		Customer update = new Customer(customer);
//		update.setMarketingConsent(true);
//		update.setDataProcessingConsent(true);
//
//		Customer updatedCustomer = new Customer(original);
//		updatedCustomer.setMarketingConsent(true);
//		updatedCustomer.setDataProcessingConsent(true);
//
//		when(customerRepository.getCustomerById(customerId)).thenReturn(Mono.just(original));
//		when(customerRepository.updateCustomer(eq(customerId), any(Customer.class)))
//				.thenReturn(Mono.just(updatedCustomer));
//
//		StepVerifier.create(customerService.updateCustomer(customerId, update))
//				.expectNextMatches(c -> c.isMarketingConsent() && c.isDataProcessingConsent())
//				.verifyComplete();
//
//		verify(eventPublisher).publish(updatedEventCaptor.capture());
//		CustomerUpdatedEvent event = updatedEventCaptor.getValue();
//		assertEquals(customerId, event.getCustomerId());
//		assertTrue(event.getChanges().containsKey("marketingConsent"));
//		assertTrue(event.getChanges().containsKey("dataProcessingConsent"));
//		assertEquals(true, event.getChanges().get("marketingConsent"));
//		assertEquals(true, event.getChanges().get("dataProcessingConsent"));
//	}
//
//	@Test
//	public void testUpdateWithGeoLocationData() {
//		Customer original = new Customer(customer);
//		original.setGeoLocationData(null);
//
//		GeoLocationData geoData = new GeoLocationData("Poland", "Warsaw", "Mazovia", "00-001");
//
//		Customer update = new Customer(customer);
//		update.setGeoLocationData(geoData);
//
//		Customer updatedCustomer = new Customer(original);
//		updatedCustomer.setGeoLocationData(geoData);
//
//		when(customerRepository.getCustomerById(customerId)).thenReturn(Mono.just(original));
//		when(customerRepository.updateCustomer(eq(customerId), any(Customer.class)))
//				.thenReturn(Mono.just(updatedCustomer));
//
//		StepVerifier.create(customerService.updateCustomer(customerId, update))
//				.expectNextMatches(c ->
//						c.getGeoLocationData() != null &&
//								c.getGeoLocationData().getCity().equals("Warsaw")
//				)
//				.verifyComplete();
//
//		verify(eventPublisher).publish(updatedEventCaptor.capture());
//		CustomerUpdatedEvent event = updatedEventCaptor.getValue();
//		assertEquals(customerId, event.getCustomerId());
//		assertNotNull(event.getChanges());
//	}
//}