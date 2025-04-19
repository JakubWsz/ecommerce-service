package pl.ecommerce.customer.write.api;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pl.ecommerce.commons.model.customer.Address;
import pl.ecommerce.commons.model.customer.AddressType;
import pl.ecommerce.commons.model.customer.CustomerConsents;
import pl.ecommerce.commons.model.customer.CustomerStatus;
import pl.ecommerce.customer.write.api.dto.*;
import pl.ecommerce.customer.write.infrastructure.repository.CustomerRepository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.ecommerce.commons.model.customer.CustomerStatus.DELETED;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class CustomerControllerTest {

	static {
		System.setProperty("testcontainers.ryuk.disabled", "true");
		System.setProperty("testcontainers.reuse.enable", "true");
	}

	@LocalServerPort
	private int port;

	private WebTestClient webTestClient;

	@Autowired
	CustomerRepository customerRepository;

	@Container
	static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
			.withDatabaseName("customer_event_store")
			.withUsername("test")
			.withPassword("test");

	@Container
	static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

	@DynamicPropertySource
	static void registerDynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
		registry.add("spring.datasource.username", postgresContainer::getUsername);
		registry.add("spring.datasource.password", postgresContainer::getPassword);
		registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
		registry.add("spring.kafka.producer.properties.max.block.ms", () -> "3000");
		registry.add("spring.kafka.producer.properties.request.timeout.ms", () -> "3000");
		registry.add("spring.kafka.producer.properties.delivery.timeout.ms", () -> "3000");
		registry.add("management.tracing.enabled", () -> "false");
	}

	@BeforeAll
	static void beforeAll() {
		postgresContainer.start();
		kafkaContainer.start();
	}

	@AfterAll
	static void afterAll() {
		postgresContainer.stop();
		kafkaContainer.stop();
	}

	@BeforeEach
	void setUp() {
		webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + port + "/api/v1/customers")
				.responseTimeout(Duration.ofSeconds(30))
				.build();
	}

	@Test
	void shouldRegisterCustomerSuccessfully() {
		var request = new CustomerRegistrationRequest(
				"test@example.com",
				"John",
				"Doe",
				"+1234567890",
				"Password123",
				CustomerConsents.builder()
						.gdprConsent(true)
						.marketingConsent(true)
						.dataProcessingConsent(true)
						.build()
		);

		webTestClient.post()
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(request)
				.exchange()
				.expectStatus().isCreated();

		var saved = customerRepository.findByEmail(request.email()).block();
		assertThat(saved).isNotNull();
		assertThat(saved.getEmail()).isEqualTo(request.email());
		assertThat(saved.getFirstName()).isEqualTo(request.firstName());
		assertThat(saved.getLastName()).isEqualTo(request.lastName());
	}

	@Test
	void shouldFailWhenGdprConsentIsMissing() {
		var request = new CustomerRegistrationRequest(
				"test2@example.com",
				"Jane",
				"Smith",
				"+1234567890",
				"Password123",
				CustomerConsents.builder()
						.gdprConsent(false)
						.marketingConsent(true)
						.dataProcessingConsent(true)
						.build()
		);

		webTestClient.post()
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(request)
				.exchange()
				.expectStatus().isForbidden();

		var saved = customerRepository.findByEmail(request.email()).block();
		assertThat(saved).isNull();
	}

	@Test
	void shouldChangeEmailSuccessfully() {
		String oldEmail = "email-change-" + UUID.randomUUID() + "@example.com";
		var regReq = new CustomerRegistrationRequest(
				oldEmail,
				"Email",
				"Change",
				"+1234567890",
				"Password123",
				CustomerConsents.builder().gdprConsent(true).marketingConsent(true).dataProcessingConsent(true).build()
		);
		webTestClient.post().contentType(MediaType.APPLICATION_JSON).bodyValue(regReq).exchange().expectStatus().isCreated();
		var saved = customerRepository.findByEmail(oldEmail).block();
		assertThat(saved).isNotNull();
		UUID customerId = saved.getId();

		String newEmail = "new-email-" + UUID.randomUUID() + "@example.com";

		webTestClient.put()
				.uri(uriBuilder -> uriBuilder.path("/{id}/email").queryParam("newEmail", newEmail).build(customerId))
				.exchange()
				.expectStatus().isNoContent();

		var updated = customerRepository.findById(customerId).block();
		assertThat(updated).isNotNull();
		assertThat(updated.getEmail()).isEqualTo(newEmail);
	}

	@Test
	void shouldAddShippingAddressSuccessfully() {
		String email = "address-test-" + UUID.randomUUID() + "@example.com";
		var regReq = new CustomerRegistrationRequest(
				email,
				"Address",
				"Test",
				"+1234567890",
				"Password123",
				CustomerConsents.builder().gdprConsent(true).marketingConsent(true).dataProcessingConsent(true).build()
		);
		webTestClient.post().contentType(MediaType.APPLICATION_JSON).bodyValue(regReq).exchange().expectStatus().isCreated();
		var customer = customerRepository.findByEmail(email).block();
		assertThat(customer).isNotNull();
		UUID customerId = customer.getId();

		var addressReq = new AddShippingAddressRequest(
				AddressType.SHIPPING.name(),
				"123",
				"45",
				"Main St",
				"New York",
				"10001",
				"USA",
				"NY",
				true
		);

		webTestClient.post()
				.uri("/{id}/addresses", customerId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(addressReq)
				.exchange()
				.expectStatus().isNoContent();

		var updated = customerRepository.findById(customerId).block();
		assertThat(updated).isNotNull();
		assertThat(updated.getShippingAddresses()).hasSize(1);
		var addr = updated.getShippingAddresses().getFirst();
		assertThat(addr.getStreet()).isEqualTo(addressReq.street());
		assertThat(addr.getCity()).isEqualTo(addressReq.city());
		assertThat(addr.getPostalCode()).isEqualTo(addressReq.postalCode());
		assertThat(addr.getCountry()).isEqualTo(addressReq.country());
	}

	@Test
	void shouldUpdatePreferencesSuccessfully() {
		String email = "preferences-test-" + UUID.randomUUID() + "@example.com";
		var regReq = new CustomerRegistrationRequest(
				email,
				"Preferences",
				"Test",
				"+1234567890",
				"Password123",
				CustomerConsents.builder().gdprConsent(true).marketingConsent(true).dataProcessingConsent(true).build()
		);
		webTestClient.post().contentType(MediaType.APPLICATION_JSON).bodyValue(regReq).exchange().expectStatus().isCreated();
		var customer = customerRepository.findByEmail(email).block();
		assertThat(customer).isNotNull();
		UUID customerId = customer.getId();

		var prefReq = new UpdatePreferencesRequest(
				true,
				false,
				"en",
				"USD",
				List.of("electronics", "books")
		);

		webTestClient.put()
				.uri("/{id}/preferences", customerId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(prefReq)
				.exchange()
				.expectStatus().isNoContent();

		var updated = customerRepository.findById(customerId).block();
		assertThat(updated).isNotNull();
		assertThat(updated.getPreferences()).isNotNull();
		assertThat(updated.getPreferences().isMarketingConsent()).isEqualTo(prefReq.marketingConsent());
		assertThat(updated.getPreferences().isNewsletterSubscribed()).isEqualTo(prefReq.newsletterSubscribed());
		assertThat(updated.getPreferences().getPreferredLanguage()).isEqualTo(prefReq.preferredLanguage());
		assertThat(updated.getPreferences().getPreferredCurrency()).isEqualTo(prefReq.preferredCurrency());
		assertThat(updated.getPreferences().getFavoriteCategories()).containsExactlyElementsOf(prefReq.favoriteCategories());
	}

	@Test
	void shouldDeactivateAndReactivateCustomerSuccessfully() {
		String email = "status-test-" + UUID.randomUUID() + "@example.com";
		var regReq = new CustomerRegistrationRequest(
				email,
				"Status",
				"Test",
				"+1234567890",
				"Password123",
				CustomerConsents.builder().gdprConsent(true).marketingConsent(true).dataProcessingConsent(true).build()
		);
		webTestClient.post().contentType(MediaType.APPLICATION_JSON).bodyValue(regReq).exchange().expectStatus().isCreated();
		var customer = customerRepository.findByEmail(email).block();
		assertThat(customer).isNotNull();
		UUID customerId = customer.getId();

		webTestClient.post()
				.uri(uriBuilder -> uriBuilder.path("/{id}/deactivate").queryParam("reason", "Customer requested deactivation").build(customerId))
				.exchange()
				.expectStatus().isNoContent();

		var deactivated = customerRepository.findById(customerId).block();
		assertThat(deactivated).isNotNull();
		assertThat(deactivated.getStatus() == CustomerStatus.INACTIVE).isTrue();

		webTestClient.post()
				.uri(uriBuilder -> uriBuilder.path("/{id}/reactivate").queryParam("note", "Customer requested reactivation").build(customerId))
				.exchange()
				.expectStatus().isNoContent();

		var reactivated = customerRepository.findById(customerId).block();
		assertThat(reactivated).isNotNull();
		assertThat(reactivated.getStatus() == CustomerStatus.ACTIVE).isTrue();
	}

	@Test
	void shouldHandleCompleteCustomerLifecycle() {
		String uniqueId = UUID.randomUUID().toString().substring(0, 8);
		String email = "lifecycle-" + uniqueId + "@example.com";

		var regReq = new CustomerRegistrationRequest(
				email,
				"Lifecycle",
				"Test",
				"+1234567890",
				"Password123",
				CustomerConsents.builder().gdprConsent(true).marketingConsent(true).dataProcessingConsent(true).build()
		);
		webTestClient.post()
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(regReq)
				.exchange()
				.expectStatus().isCreated();


		var customer = customerRepository.findByEmail(email).block();
		assertThat(customer).isNotNull();
		UUID customerId = customer.getId();

		var secondAddrReq = new AddShippingAddressRequest(
				AddressType.SHIPPING.name(),
				"789",
				"101",
				"Second St",
				"Second City",
				"67890",
				"Second Country",
				"Second Region",
				true
		);
		webTestClient.post()
				.uri("/{id}/addresses", customerId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(secondAddrReq)
				.exchange()
				.expectStatus().isNoContent();



		var withSecondAddr = customerRepository.findById(customerId).block();
		assertThat(withSecondAddr).isNotNull();
		assertThat(withSecondAddr.getShippingAddresses()).hasSize(1);
		UUID secondAddressId = withSecondAddr.getShippingAddresses().getFirst().getId();

		var firstAddrReq = new AddShippingAddressRequest(
				AddressType.SHIPPING.name(),
				"123",
				"45",
				"Lifecycle St",
				"Test City",
				"12345",
				"Test Country",
				"Test Region",
				false
		);
		webTestClient.post()
				.uri("/{id}/addresses", customerId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(firstAddrReq)
				.exchange()
				.expectStatus().isNoContent();



		var withTwoAddresses = customerRepository.findById(customerId).block();
		assertThat(withTwoAddresses).isNotNull();
		assertThat(withTwoAddresses.getShippingAddresses()).hasSize(2);

		UUID firstAddressId = withTwoAddresses.getShippingAddresses().stream()
				.map(Address::getId)
				.filter(id -> !id.equals(secondAddressId))
				.findFirst()
				.orElseThrow();

		assertThat(withTwoAddresses.getDefaultShippingAddressId()).isEqualTo(secondAddressId);

		webTestClient.delete()
				.uri("/{id}/addresses/{addressId}", customerId, firstAddressId)
				.exchange()
				.expectStatus().isNoContent();


		var afterFirstRemove = customerRepository.findById(customerId).block();
		assertThat(afterFirstRemove).isNotNull();
		assertThat(afterFirstRemove.getShippingAddresses()).hasSize(1);

		var updateSecondAddrReq = new UpdateShippingAddressRequest(
				"789",
				"101",
				"Second St",
				"Second City",
				"67890",
				"Second Country",
				"Second Region",
				false
		);
		webTestClient.put()
				.uri("/{id}/addresses/{addressId}", customerId, secondAddressId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(updateSecondAddrReq)
				.exchange()
				.expectStatus().isNoContent();



		webTestClient.post()
				.uri(uriBuilder -> uriBuilder.path("/{id}/deactivate").queryParam("reason", "Lifecycle test deactivation").build(customerId))
				.exchange()
				.expectStatus().isNoContent();


		var deactivated = customerRepository.findById(customerId).block();
		assertThat(deactivated).isNotNull();
		assertThat(deactivated.getStatus() == CustomerStatus.ACTIVE).isFalse();

		webTestClient.delete()
				.uri("/{id}", customerId)
				.exchange()
				.expectStatus().isNoContent();


		var deleted = customerRepository.findById(customerId).block();
		assertThat(deleted.getStatus() == DELETED).isTrue();
	}
}
