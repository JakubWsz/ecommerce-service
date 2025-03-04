package pl.ecommerce.customer.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pl.ecommerce.commons.event.customer.CustomerRegisteredEvent;
import pl.ecommerce.commons.event.customer.CustomerUpdatedEvent;
import pl.ecommerce.commons.event.customer.CustomerDeletedEvent;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.customer.domain.model.*;
import pl.ecommerce.customer.domain.repository.CustomerRepository;
import pl.ecommerce.customer.domain.repository.CustomerRepositoryMongo;
import pl.ecommerce.customer.domain.service.CustomerService;
import pl.ecommerce.customer.infrastructure.client.GeolocationClient;
import pl.ecommerce.customer.infrastructure.client.dto.GeoLocationResponse;
import pl.ecommerce.customer.infrastructure.exception.CustomerAlreadyExistsException;
import pl.ecommerce.customer.infrastructure.exception.CustomerNotFoundException;
import pl.ecommerce.customer.infrastructure.exception.GdprConsentRequiredException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@EmbeddedKafka(partitions = 1, topics = {"customer-events"})
@ActiveProfiles("test")
public class CustomerServiceIntegrationTest {

	@Container
	private static final MongoDBContainer MONGO_DB = new MongoDBContainer(
			DockerImageName.parse("mongo:6-focal"))
			.withExposedPorts(27017)
			.withStartupTimeout(Duration.ofSeconds(60))
			.waitingFor(Wait.forListeningPort());

	@Container
	private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer("apache/kafka-native:3.8.0");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", () -> String.format(
				"mongodb://%s:%d/customer-service-test",
				MONGO_DB.getHost(), MONGO_DB.getFirstMappedPort()));
		registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
	}

	@Autowired
	private ReactiveMongoTemplate template;

	private CustomerRepository customerRepository;

	@Mock
	private GeolocationClient geolocationClient;

	@Mock
	private EventPublisher eventPublisher;

	private CustomerService customerService;

	private static final UUID CUSTOMER_ID = UUID.randomUUID();
	private static final String CUSTOMER_EMAIL = "john.doe@example.com";
	private Customer testCustomer;

	@BeforeEach
	void setupBeforeEach() {
		MockitoAnnotations.openMocks(this);

		PersonalData personalData = new PersonalData(CUSTOMER_EMAIL, "John", "Doe", "123456789");

		LocalDateTime now = LocalDateTime.now();
		testCustomer = new Customer(
				true,
				"127.0.0.1",
				now,
				now,
				null,
				true,
				now,
				false,
				false,
				365,
				personalData,
				null,
				null,
				null
		);
		testCustomer.setId(CUSTOMER_ID);

		template.dropCollection(Customer.class).block();
		template.createCollection(Customer.class).block();
		customerRepository = new CustomerRepositoryMongo(template);
		customerService = new CustomerService(customerRepository, geolocationClient, eventPublisher);
	}

	@BeforeAll
	static void setupBeforeAll() {
		MONGO_DB.start();
		KAFKA_CONTAINER.start();
	}

	@AfterAll
	static void afterAll() {
		MONGO_DB.stop();
		MONGO_DB.close();
		KAFKA_CONTAINER.stop();
		KAFKA_CONTAINER.close();
	}
	@Test
	void deleteCustomer_ExistingCustomer_ShouldDeactivateSuccessfully() {
		customerRepository.saveCustomer(testCustomer).block();
		doNothing().when(eventPublisher).publish(any(CustomerDeletedEvent.class));

		StepVerifier.create(customerService.deleteCustomer(CUSTOMER_ID)
						.then(customerRepository.getCustomerById(CUSTOMER_ID)))
				.assertNext(customer -> {
					assertThat(customer).isNotNull();
					assertThat(customer.getId()).isEqualTo(CUSTOMER_ID);
					assertThat(customer.isActive()).isFalse();
				})
				.verifyComplete();

		verify(eventPublisher, times(1)).publish(any(CustomerDeletedEvent.class));
	}

	@Test
	void registerCustomer_WithValidData_ShouldRegisterSuccessfully() {
		String ipAddress = "192.168.1.1";
		Map<String, String> currencyMap = Map.of("USD", "1.0");

		GeoLocationResponse geoResponse = new GeoLocationResponse();
		geoResponse.setCountry("US");
		geoResponse.setCity("New York");
		geoResponse.setState("NY");
		geoResponse.setPostalCode("10001");
		geoResponse.setCurrency(currencyMap);

		when(geolocationClient.getLocationByIp(ipAddress)).thenReturn(Mono.just(geoResponse));
		doNothing().when(eventPublisher).publish(any(CustomerRegisteredEvent.class));

		PersonalData personalData = new PersonalData("jane.doe@example.com", "Jane", "Doe", "987654321");
		LocalDateTime now = LocalDateTime.now();
		Customer newCustomer = new Customer(
				false,
				null,
				null,
				null,
				null,
				true,
				null,
				false,
				false,
				null,
				personalData,
				null,
				null,
				null
		);

		StepVerifier.create(customerService.registerCustomer(newCustomer, ipAddress))
				.assertNext(customer -> {
					assertThat(customer).isNotNull();
					assertThat(customer.getId()).isNotNull();
					assertThat(customer.getPersonalData().getEmail()).isEqualTo("jane.doe@example.com");
					assertThat(customer.isActive()).isTrue();
					assertThat(customer.getGeoLocationData()).isNotNull();
					assertThat(customer.getGeoLocationData().getCountry()).isEqualTo("US");
				})
				.verifyComplete();

		verify(eventPublisher, times(1)).publish(any(CustomerRegisteredEvent.class));
		verify(geolocationClient, times(1)).getLocationByIp(ipAddress);
	}

	@Test
	void registerCustomer_WithoutGdprConsent_ShouldFail() {
		PersonalData personalData = new PersonalData("test@example.com", "Test", "User", "123123123");
		LocalDateTime now = LocalDateTime.now();
		Customer invalidCustomer = new Customer(
				false,
				null,
				null,
				null,
				null,
				false,
				null,
				false,
				false,
				null,
				personalData,
				null,
				null,
				null
		);

		Exception exception = null;
		try {
			var c = customerService.registerCustomer(invalidCustomer, "127.0.0.1").block();

		}catch (Exception e){
			exception =e;
		}

		assertNotNull(exception);
		assertInstanceOf(GdprConsentRequiredException.class, exception);
		assertEquals("GDPR consent is required", exception.getMessage());

		verify(eventPublisher, never()).publish(any());
		verify(geolocationClient, never()).getLocationByIp(anyString());
	}

	@Test
	void registerCustomer_WithExistingEmail_ShouldFail() {

		customerRepository.saveCustomer(testCustomer).block();

		PersonalData duplicatePersonalData = new PersonalData(CUSTOMER_EMAIL, "Another", "User", "555666777");
		LocalDateTime now = LocalDateTime.now();
		Customer duplicateCustomer = new Customer(
				false,
				null,
				null,
				null,
				null,
				true,
				null,
				false,
				false,
				null,
				duplicatePersonalData,
				null,
				null,
				null
		);

		StepVerifier.create(customerService.registerCustomer(duplicateCustomer, "127.0.0.1"))
				.expectErrorMatches(e -> e.getMessage().contains("Customer with this email already exists"))
				.verify();

		verify(eventPublisher, never()).publish(any());
	}

	@Test
	void getCustomerById_ExistingCustomer_ShouldReturnCustomer() {
		customerRepository.saveCustomer(testCustomer).block();

		StepVerifier.create(customerService.getCustomerById(CUSTOMER_ID))
				.assertNext(customer -> {
					assertThat(customer).isNotNull();
					assertThat(customer.getId()).isEqualTo(CUSTOMER_ID);
					assertThat(customer.getPersonalData().getEmail()).isEqualTo(CUSTOMER_EMAIL);
				})
				.verifyComplete();
	}

	@Test
	void getCustomerById_NonExistingCustomer_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		StepVerifier.create(customerService.getCustomerById(nonExistingId))
				.expectErrorMatches(throwable -> throwable instanceof CustomerNotFoundException)
				.verify();
	}

	@Test
	void updateCustomer_ExistingCustomer_ShouldUpdateSuccessfully() {
		customerRepository.saveCustomer(testCustomer).block();

		PersonalData updatedPersonalData = new PersonalData(CUSTOMER_EMAIL, "John", "Updated", "999888777");
		LocalDateTime now = LocalDateTime.now();
		Customer customerUpdate = new Customer(
				false,
				null,
				null,
				null,
				null,
				true,
				null,
				true,
				false,
				null,
				updatedPersonalData,
				null,
				null,
				null
		);

		doNothing().when(eventPublisher).publish(any(CustomerUpdatedEvent.class));

		StepVerifier.create(customerService.updateCustomer(CUSTOMER_ID, customerUpdate))
				.assertNext(customer -> {
					assertThat(customer).isNotNull();
					assertThat(customer.getId()).isEqualTo(CUSTOMER_ID);
					assertThat(customer.getPersonalData().getLastName()).isEqualTo("Updated");
					assertThat(customer.isMarketingConsent()).isTrue();
				})
				.verifyComplete();

		verify(eventPublisher, times(1)).publish(any(CustomerUpdatedEvent.class));
	}

	@Test
	void updateCustomer_WithAddresses_ShouldUpdateSuccessfully() {
		List<Address> addresses = new ArrayList<>();
		addresses.add(new Address("123 Main St", "101", null, "New York", "NY", "10001", "US", true, AddressType.SHIPPING));

		Customer customerUpdate = new Customer(
				false,
				null,
				null,
				null,
				null,
				false,
				null,
				false,
				false,
				null,
				null,
				addresses,
				null,
				null
		);

		doNothing().when(eventPublisher).publish(any(CustomerUpdatedEvent.class));

		StepVerifier.create(customerService.updateCustomer(CUSTOMER_ID, customerUpdate))
				.assertNext(customer -> {
					assertThat(customer).isNotNull();
					assertThat(customer.getId()).isEqualTo(CUSTOMER_ID);
					assertThat(customer.getAddresses()).hasSize(1);
					assertThat(customer.getAddresses().getFirst().getCity()).isEqualTo("New York");
				})
				.verifyComplete();

		verify(eventPublisher, times(1)).publish(any(CustomerUpdatedEvent.class));
	}

	@Test
	void updateCustomer_NonExistingCustomer_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();
		PersonalData personalData = new PersonalData("new@example.com", "New", "Name", "123321123");

		Customer customerUpdate = new Customer(
				false,
				null,
				null,
				null,
				null,
				true,
				null,
				false,
				false,
				null,
				personalData,
				null,
				null,
				null
		);

		StepVerifier.create(customerService.updateCustomer(nonExistingId, customerUpdate))
				.expectErrorMatches(throwable -> throwable instanceof CustomerNotFoundException)
				.verify();

		verify(eventPublisher, never()).publish(any());
	}

	@Test
	void deleteCustomer_NonExistingCustomer_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		StepVerifier.create(customerService.deleteCustomer(nonExistingId))
				.expectErrorMatches(throwable -> throwable instanceof CustomerNotFoundException)
				.verify();

		verify(eventPublisher, never()).publish(any());
	}
}