package pl.ecommerce.customer.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pl.ecommerce.customer.domain.model.Customer;
import pl.ecommerce.customer.domain.model.PersonalData;
import pl.ecommerce.customer.domain.service.CustomerService;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class CustomerIntegrationTest {

	@Container
	private static final MongoDBContainer MONGO_DB = new MongoDBContainer(
			DockerImageName.parse("mongo:6-focal"))
			.withExposedPorts(27017);

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", () -> String.format(
				"mongodb://%s:%d/customer-service-test",
				MONGO_DB.getHost(), MONGO_DB.getFirstMappedPort()));
	}

	@Autowired
	private CustomerService customerService;

	@Test
	public void testRegisterAndGetCustomer() {
		var pd = new PersonalData("integration@example.com", "Integration", "Test", "555123456");
		var customer = new Customer(
				true, "127.0.0.1", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
				true, LocalDateTime.now(), false, false, 730,
				pd, null, null, null
		);

		StepVerifier.create(customerService.registerCustomer(customer, "127.0.0.1"))
				.assertNext(savedCustomer -> {
					assert savedCustomer.getId() != null;
					assert savedCustomer.getPersonalData() != null;
				})
				.verifyComplete();

		StepVerifier.create(customerService.getCustomerById(customer.getId()))
				.assertNext(fetched -> {
					assert fetched.getId().equals(customer.getId());
				})
				.verifyComplete();
	}

	@Test
	public void testUpdateCustomerIntegration() {
		var pd = new PersonalData("update@example.com", "Update", "Test", "555000111");
		var customer = new Customer(
				true, "127.0.0.1", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
				true, LocalDateTime.now(), false, false, 730,
				pd, null, null, null
		);

		// Save the customer ID since it's set when created
		UUID customerId = customer.getId();

		// Using Block to ensure registration completes before update
		Customer savedCustomer = customerService.registerCustomer(customer, "127.0.0.1")
				.block(Duration.ofSeconds(10));

		assert savedCustomer != null;
		assert savedCustomer.getId() != null;
		assert savedCustomer.getId().equals(customerId);

		// Create a updated personal data
		var updatedPd = new PersonalData("update_new@example.com", "UpdateNew", "Test", "555222333");

		// Create a new customer instance for the update
		var customerToUpdate = new Customer(
				savedCustomer.isActive(),
				savedCustomer.getRegistrationIp(),
				savedCustomer.getCreatedAt(),
				LocalDateTime.now(), // update time
				savedCustomer.getLastLoginAt(),
				savedCustomer.isGdprConsent(),
				savedCustomer.getConsentTimestamp(),
				savedCustomer.isMarketingConsent(),
				savedCustomer.isDataProcessingConsent(),
				savedCustomer.getDataRetentionPeriodDays(),
				updatedPd, // updated personal data
				savedCustomer.getAddresses(),
				savedCustomer.getGeoLocationData(),
				savedCustomer.getCurrencies()
		);
		customerToUpdate.setId(customerId); // Set the ID to ensure update not insert

		// Update the customer and verify
		StepVerifier.create(customerService.updateCustomer(customerId, customerToUpdate))
				.assertNext(updated -> {
					assert updated.getPersonalData().getEmail().equals("update_new@example.com");
					assert updated.getPersonalData().getFirstName().equals("UpdateNew");
				})
				.verifyComplete();

		// Verify the update by retrieving the customer
		StepVerifier.create(customerService.getCustomerById(customerId))
				.assertNext(retrieved -> {
					assert retrieved.getPersonalData().getEmail().equals("update_new@example.com");
					assert retrieved.getPersonalData().getFirstName().equals("UpdateNew");
				})
				.verifyComplete();
	}


	@Test
	public void testDeleteCustomerIntegration() {
		var pd = new PersonalData("delete@example.com", "Delete", "Test", "555333444");
		var customer = new Customer(
				true, "127.0.0.1", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
				true, LocalDateTime.now(), false, false, 730,
				pd, null, null, null
		);

		StepVerifier.create(customerService.registerCustomer(customer, "127.0.0.1"))
				.assertNext(savedCustomer -> {
					assert savedCustomer.getId() != null;
				})
				.verifyComplete();

		StepVerifier.create(customerService.deleteCustomer(customer.getId()))
				.verifyComplete();

		StepVerifier.create(customerService.getCustomerById(customer.getId()))
				.assertNext(fetched -> {
					assert !fetched.isActive();
				})
				.verifyComplete();
	}
}