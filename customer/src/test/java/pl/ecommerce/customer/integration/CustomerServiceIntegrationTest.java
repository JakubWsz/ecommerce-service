//package pl.ecommerce.customer.integration;
//
//import org.apache.kafka.clients.admin.AdminClient;
//import org.apache.kafka.clients.admin.AdminClientConfig;
//import org.apache.kafka.clients.admin.NewTopic;
//import org.apache.kafka.clients.consumer.KafkaConsumer;
//import org.apache.kafka.common.errors.TopicExistsException;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.wait.strategy.Wait;
//import org.testcontainers.kafka.KafkaContainer;
//import org.testcontainers.containers.MongoDBContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.utility.DockerImageName;
//import pl.ecommerce.commons.kafka.EventPublisher;
//import pl.ecommerce.customer.TestEventListener;
//import pl.ecommerce.customer.domain.model.*;
//import pl.ecommerce.customer.domain.repository.CustomerRepository;
//import pl.ecommerce.customer.domain.repository.CustomerRepositoryMongo;
//import pl.ecommerce.customer.domain.service.CustomerService;
//import pl.ecommerce.customer.infrastructure.client.GeolocationClient;
//import pl.ecommerce.customer.infrastructure.client.dto.GeoLocationResponse;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
//
//@SpringBootTest
//@Testcontainers
//@ExtendWith(MockitoExtension.class)
//@ActiveProfiles("test")
//public class CustomerServiceIntegrationTest {
//
//	@Container
//	private static final MongoDBContainer MONGO_DB = new MongoDBContainer(
//			DockerImageName.parse("mongo:6-focal"))
//			.withExposedPorts(27017)
//			.withStartupTimeout(Duration.ofSeconds(60))
//			.waitingFor(Wait.forListeningPort());
//
//	@Container
//	private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
//			DockerImageName.parse("apache/kafka-native:3.8.0"))
//			.withStartupTimeout(Duration.ofMinutes(2))
//			.withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
//			.withEnv("KAFKA_NUM_PARTITIONS", "1")
//			.withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
//			.withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
//			.withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
//			.withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", "1")
//			.withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
//
//	private static KafkaConsumer<String, String> consumer;
//	private static final String[] TOPICS = {
//			"customer.registered.event",
//			"customer.updated.event",
//			"customer.deleted.event"
//	};
//
//	@DynamicPropertySource
//	static void configureProperties(DynamicPropertyRegistry registry) {
//		registry.add("spring.data.mongodb.uri", () -> String.format(
//				"mongodb://%s:%d/customer-service-test",
//				MONGO_DB.getHost(), MONGO_DB.getFirstMappedPort()));
//		registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
//	}
//
//	@Autowired
//	private ReactiveMongoTemplate template;
//
//	@Autowired
//	private CustomerRepository customerRepository;
//
//	@Mock
//	private GeolocationClient geolocationClient;
//
//	@Autowired
//	private TestEventListener testEventListener;
//
//	@Autowired
//	private EventPublisher eventPublisher;
//
//	@Autowired
//	private CustomerService customerService;
//
//	private static final UUID CUSTOMER_ID = UUID.randomUUID();
//	private static final String CUSTOMER_EMAIL = "john.doe@example.com";
//	private Customer testCustomer;
//
//	@BeforeEach
//	void setupBeforeEach() {
//
//		PersonalData personalData = new PersonalData(CUSTOMER_EMAIL, "John", "Doe", "123456789");
//
//		LocalDateTime now = LocalDateTime.now();
//		testCustomer = new Customer(
//				true,
//				"127.0.0.1",
//				now,
//				now,
//				null,
//				true,
//				now,
//				false,
//				false,
//				365,
//				personalData,
//				null,
//				null,
//				null
//		);
//		testCustomer.setId(CUSTOMER_ID);
//
//		template.dropCollection(Customer.class).block();
//		template.createCollection(Customer.class).block();
//		customerRepository = new CustomerRepositoryMongo(template);
//		customerService = new CustomerService(customerRepository, geolocationClient, eventPublisher);
//	}
//
//	@BeforeAll
//	static void setupBeforeAll() {
//		MONGO_DB.start();
//		KAFKA_CONTAINER.start();
//
//		waitForKafkaReady();
//	}
//
//	@AfterAll
//	static void afterAll() {
//		if (consumer != null) {
//			consumer.close();
//		}
//
//		MONGO_DB.stop();
//		MONGO_DB.close();
//		KAFKA_CONTAINER.stop();
//		KAFKA_CONTAINER.close();
//	}
//	@Test
//	void deleteCustomer_ExistingCustomer_ShouldDeactivateSuccessfully() {
//		customerRepository.saveCustomer(testCustomer).block();
//
//		StepVerifier.create(customerService.deleteCustomer(CUSTOMER_ID)
//						.then(customerRepository.getCustomerById(CUSTOMER_ID)))
//				.assertNext(customer -> {
//					assertThat(customer).isNotNull();
//					assertThat(customer.getId()).isEqualTo(CUSTOMER_ID);
//					assertThat(customer.isActive()).isFalse();
//				})
//				.verifyComplete();
//
//		var deletedEvents = testEventListener.getCapturedEvents(CustomerDeletedEvent.class);
//		var deletedEvent = deletedEvents.getFirst();
//		assertThat(deletedEvents.size()).isEqualTo(1);
//		assertEquals(CUSTOMER_ID, deletedEvent.getCustomerId());
//		assertEquals(CUSTOMER_EMAIL, deletedEvent.getEmail());
//		assertEquals(testCustomer.getPersonalData().getLastName(), deletedEvent.getLastName());
//		assertEquals(testCustomer.getPersonalData().getFirstName(), deletedEvent.getFirstName());
//		testEventListener.clearEvents();
//	}
//
//	@Test
//	void registerCustomer_WithValidData_ShouldRegisterSuccessfully() {
//		String ipAddress = "192.168.1.1";
//		Map<String, String> currencyMap = Map.of("USD", "1.0");
//
//		GeoLocationResponse geoResponse = new GeoLocationResponse();
//		geoResponse.setCountry("US");
//		geoResponse.setCity("New York");
//		geoResponse.setState("NY");
//		geoResponse.setPostalCode("10001");
//		geoResponse.setCurrency(currencyMap);
//
//		when(geolocationClient.getLocationByIp(ipAddress)).thenReturn(Mono.just(geoResponse));
//
//		PersonalData personalData = new PersonalData("jane.doe@example.com", "Jane", "Doe", "987654321");
//		Customer newCustomer = new Customer(
//				false,
//				null,
//				null,
//				null,
//				null,
//				true,
//				null,
//				false,
//				false,
//				null,
//				personalData,
//				null,
//				null,
//				null
//		);
//
//		StepVerifier.create(customerService.registerCustomer(newCustomer, ipAddress))
//				.assertNext(customer -> {
//					assertThat(customer).isNotNull();
//					assertThat(customer.getId()).isNotNull();
//					assertThat(customer.getPersonalData().getEmail()).isEqualTo("jane.doe@example.com");
//					assertThat(customer.isActive()).isTrue();
//					assertThat(customer.getGeoLocationData()).isNotNull();
//					assertThat(customer.getGeoLocationData().getCountry()).isEqualTo("US");
//				})
//				.verifyComplete();
//
//		await().atMost(5, TimeUnit.SECONDS)
//				.until(() -> testEventListener.getEventCount(CustomerRegisteredEvent.class) > 0);
//
//		List<CustomerRegisteredEvent> registeredEvents = testEventListener.getCapturedEvents(CustomerRegisteredEvent.class);
//
//		var registeredEvent = registeredEvents.getFirst();
//		assertThat(registeredEvents.size()).isEqualTo(1);
//		assertEquals(newCustomer.getId(), registeredEvent.getCustomerId());
//		assertEquals(newCustomer.getPersonalData().getEmail(), registeredEvent.getEmail());
//		assertEquals(newCustomer.getPersonalData().getLastName(), registeredEvent.getLastName());
//		assertEquals(newCustomer.getPersonalData().getFirstName(), registeredEvent.getFirstName());
//		testEventListener.clearEvents();
//
//		verify(geolocationClient, times(1)).getLocationByIp(ipAddress);
//	}
//
//	@Test
//	void registerCustomer_WithoutGdprConsent_ShouldFail() {
//		PersonalData personalData = new PersonalData("test@example.com", "Test", "User", "123123123");
//		Customer invalidCustomer = new Customer(
//				false,
//				null,
//				null,
//				null,
//				null,
//				false,
//				null,
//				false,
//				false,
//				null,
//				personalData,
//				null,
//				null,
//				null
//		);
//
//		Exception exception = null;
//		try {
//			customerService.registerCustomer(invalidCustomer, "127.0.0.1").block();
//
//		}catch (Exception e){
//			exception =e;
//		}
//
//		assertNotNull(exception);
//		assertInstanceOf(GdprConsentRequiredException.class, exception);
//		assertEquals("GDPR consent is required", exception.getMessage());
//
//		verify(geolocationClient, never()).getLocationByIp(anyString());
//	}
//
//	@Test
//	void registerCustomer_WithExistingEmail_ShouldFail() {
//
//		customerRepository.saveCustomer(testCustomer).block();
//
//		PersonalData duplicatePersonalData = new PersonalData(CUSTOMER_EMAIL, "Another", "User", "555666777");
//		LocalDateTime now = LocalDateTime.now();
//		Customer duplicateCustomer = new Customer(
//				false,
//				null,
//				null,
//				null,
//				null,
//				true,
//				null,
//				false,
//				false,
//				null,
//				duplicatePersonalData,
//				null,
//				null,
//				null
//		);
//
//		StepVerifier.create(customerService.registerCustomer(duplicateCustomer, "127.0.0.1"))
//				.expectErrorMatches(e -> e.getMessage().contains("Customer with this email already exists"))
//				.verify();
//	}
//
//	@Test
//	void getCustomerById_ExistingCustomer_ShouldReturnCustomer() {
//		customerRepository.saveCustomer(testCustomer).block();
//
//		StepVerifier.create(customerService.getCustomerById(CUSTOMER_ID))
//				.assertNext(customer -> {
//					assertThat(customer).isNotNull();
//					assertThat(customer.getId()).isEqualTo(CUSTOMER_ID);
//					assertThat(customer.getPersonalData().getEmail()).isEqualTo(CUSTOMER_EMAIL);
//				})
//				.verifyComplete();
//	}
//
//	@Test
//	void getCustomerById_NonExistingCustomer_ShouldFail() {
//		UUID nonExistingId = UUID.randomUUID();
//
//		StepVerifier.create(customerService.getCustomerById(nonExistingId))
//				.expectErrorMatches(throwable -> throwable instanceof CustomerNotFoundException)
//				.verify();
//	}
//
//	@Test
//	void updateCustomer_ExistingCustomer_ShouldUpdateSuccessfully() {
//		customerRepository.saveCustomer(testCustomer).block();
//
//		PersonalData updatedPersonalData = new PersonalData(CUSTOMER_EMAIL, "John", "Updated", "999888777");
//		Customer customerUpdate = new Customer(
//				false,
//				null,
//				null,
//				null,
//				null,
//				true,
//				null,
//				true,
//				false,
//				null,
//				updatedPersonalData,
//				null,
//				null,
//				null
//		);
//
//		StepVerifier.create(customerService.updateCustomer(CUSTOMER_ID, customerUpdate))
//				.assertNext(customer -> {
//					assertThat(customer).isNotNull();
//					assertThat(customer.getId()).isEqualTo(CUSTOMER_ID);
//					assertThat(customer.getPersonalData().getLastName()).isEqualTo("Updated");
//					assertThat(customer.isMarketingConsent()).isTrue();
//				})
//				.verifyComplete();
//
//		await().atMost(5, TimeUnit.SECONDS)
//				.until(() -> testEventListener.getEventCount(CustomerUpdatedEvent.class) > 0);
//
//		List<CustomerUpdatedEvent> customerUpdatedEvents = testEventListener.getCapturedEvents(CustomerUpdatedEvent.class);
//
//		var updatedEvent = customerUpdatedEvents.getFirst();
//		assertThat(customerUpdatedEvents.size()).isEqualTo(1);
//		assertEquals(CUSTOMER_ID, updatedEvent.getCustomerId());
//		testEventListener.clearEvents();
//	}
//
//	@Test
//	void updateCustomer_WithAddresses_ShouldUpdateSuccessfully() {
//		List<Address> addresses = new ArrayList<>();
//		addresses.add(new Address("123 Main St", "101", null, "New York", "NY", "10001", "US", true, AddressType.SHIPPING));
//
//		Customer customerUpdate = new Customer(
//				false,
//				null,
//				null,
//				null,
//				null,
//				false,
//				null,
//				false,
//				false,
//				null,
//				null,
//				addresses,
//				null,
//				null
//		);
//
//		StepVerifier.create(customerService.updateCustomer(CUSTOMER_ID, customerUpdate))
//				.assertNext(customer -> {
//					assertThat(customer).isNotNull();
//					assertThat(customer.getId()).isEqualTo(CUSTOMER_ID);
//					assertThat(customer.getAddresses()).hasSize(1);
//					assertThat(customer.getAddresses().getFirst().getCity()).isEqualTo("New York");
//				})
//				.verifyComplete();
//
//
//		await().atMost(5, TimeUnit.SECONDS)
//				.until(() -> testEventListener.getEventCount(CustomerUpdatedEvent.class) > 0);
//
//		List<CustomerUpdatedEvent> customerUpdatedEvents = testEventListener.getCapturedEvents(CustomerUpdatedEvent.class);
//
//		var updatedEvent = customerUpdatedEvents.getFirst();
//		assertThat(customerUpdatedEvents.size()).isEqualTo(1);
//		assertEquals(CUSTOMER_ID, updatedEvent.getCustomerId());
//		testEventListener.clearEvents();
//	}
//
//	@Test
//	void updateCustomer_NonExistingCustomer_ShouldFail() {
//		UUID nonExistingId = UUID.randomUUID();
//		PersonalData personalData = new PersonalData("new@example.com", "New", "Name", "123321123");
//
//		Customer customerUpdate = new Customer(
//				false,
//				null,
//				null,
//				null,
//				null,
//				true,
//				null,
//				false,
//				false,
//				null,
//				personalData,
//				null,
//				null,
//				null
//		);
//
//		StepVerifier.create(customerService.updateCustomer(nonExistingId, customerUpdate))
//				.expectErrorMatches(throwable -> throwable instanceof CustomerNotFoundException)
//				.verify();
//	}
//
//	@Test
//	void deleteCustomer_NonExistingCustomer_ShouldFail() {
//		UUID nonExistingId = UUID.randomUUID();
//
//		StepVerifier.create(customerService.deleteCustomer(nonExistingId))
//				.expectErrorMatches(throwable -> throwable instanceof CustomerNotFoundException)
//				.verify();
//	}
//
//	private static void waitForKafkaReady() {
//		int retries = 20;
//		int waitTimeMs = 5000;
//
//		while (retries-- > 0) {
//			try {
//				if (KAFKA_CONTAINER.isRunning()) {
//					try (AdminClient adminClient = createAdminClient()) {
//						List<NewTopic> newTopics = Arrays.stream(TOPICS)
//								.map(topic -> new NewTopic(topic, 1, (short) 1))
//								.collect(Collectors.toList());
//
//						try {
//							adminClient.createTopics(newTopics);
//							Thread.sleep(1000);
//
//							Set<String> existingTopics = adminClient.listTopics().names().get();
//							if (existingTopics.containsAll(Arrays.asList(TOPICS))) {
//								return;
//							}
//						} catch (Exception e) {
//							if (e instanceof TopicExistsException) {
//								return;
//							}
//							System.out.println("Error creating topics: " + e.getMessage());
//						}
//					}
//				}
//			} catch (Exception e) {
//				System.out.println("Waiting for Kafka to be ready. Retries left: " + retries);
//			}
//
//			try {
//				Thread.sleep(waitTimeMs);
//			} catch (InterruptedException ignored) {}
//		}
//
//		throw new IllegalStateException("Kafka is not ready after waiting.");
//	}
//
//	private static AdminClient createAdminClient() {
//		Properties props = new Properties();
//		props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
//		return AdminClient.create(props);
//	}
//}