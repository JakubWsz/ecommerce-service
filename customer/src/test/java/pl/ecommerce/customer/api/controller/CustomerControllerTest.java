package pl.ecommerce.customer.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.ecommerce.customer.api.dto.*;
import pl.ecommerce.customer.domain.model.Customer;
import pl.ecommerce.customer.domain.model.PersonalData;
import pl.ecommerce.customer.domain.service.CustomerService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

	@Mock
	private CustomerService customerService;

	@InjectMocks
	private CustomerController customerController;

	private WebTestClient webTestClient;
	private Customer customer;
	private CustomerRequest createRequest;
	private CustomerRequest updateRequest;
	private UUID customerId;

	@BeforeEach
	void setUp() {
		webTestClient = WebTestClient.bindToController(customerController).build();
		customerId = UUID.randomUUID();

		PersonalData personalData = new PersonalData(
				"test@example.com",
				"John",
				"Doe",
				"+48123456789"
		);

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
				365,
				personalData,
				new ArrayList<>(),
				null,
				null
		);

		customer.setId(customerId);

		createRequest = new CustomerRequest(
				new PersonalDataDto("test@example.com", "John", "Doe", "+48123456789"),
				List.of()
		);

		updateRequest = new CustomerRequest(
				new PersonalDataDto("updated@example.com", "Updated", "User", "+48987654321"),
				List.of()
		);
	}

	@Test
	void registerCustomer_ShouldReturnCreatedCustomer() {
		when(customerService.registerCustomer(any(Customer.class), any(String.class)))
				.thenReturn(Mono.just(customer));

		webTestClient.post()
				.uri("/api/v1/customers?ipAddress=127.0.0.1")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(createRequest)
				.exchange()
				.expectStatus().isOk()
				.expectBody(CustomerResponse.class)
				.value(response -> {
					assert response != null;
					assert response.personalData() != null;
					assert response.personalData().email().equals("test@example.com");
				});

		verify(customerService, times(1)).registerCustomer(any(Customer.class), eq("127.0.0.1"));
	}

	@Test
	void getCustomerById_ShouldReturnCustomer() {
		when(customerService.getCustomerById(customerId)).thenReturn(Mono.just(customer));

		webTestClient.get()
				.uri("/api/v1/customers/{id}", customerId)
				.exchange()
				.expectStatus().isOk()
				.expectBody(CustomerResponse.class)
				.value(response -> {
					assert response != null;
					assert response.personalData() != null;
					assert response.personalData().email().equals("test@example.com");
				});

		verify(customerService, times(1)).getCustomerById(customerId);
	}

	@Test
	void getAllCustomers_ShouldReturnCustomersList() {
		when(customerService.getAllCustomers()).thenReturn(Flux.just(customer));

		webTestClient.get()
				.uri("/api/v1/customers")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(CustomerResponse.class)
				.value(response -> {
					assert response != null;
					assert !response.isEmpty();
					assert response.getFirst().personalData() != null;
					assert response.getFirst().personalData().email().equals("test@example.com");
				});

		verify(customerService, times(1)).getAllCustomers();
	}

	@Test
	void updateCustomer_ShouldReturnUpdatedCustomer() {
		PersonalData updatedPersonalData = new PersonalData(
				"updated@example.com",
				"Updated",
				"User",
				"+48987654321"
		);

		Customer updatedCustomer = new Customer(
				true,
				"127.0.0.1",
				LocalDateTime.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				true,
				LocalDateTime.now(),
				false,
				false,
				365,
				updatedPersonalData,
				new ArrayList<>(),
				null,
				null
		);
		updatedCustomer.setId(customerId);

		when(customerService.updateCustomer(eq(customerId), any(Customer.class)))
				.thenReturn(Mono.just(updatedCustomer));

		webTestClient.put()
				.uri("/api/v1/customers/{id}", customerId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(updateRequest)
				.exchange()
				.expectStatus().isOk()
				.expectBody(CustomerResponse.class)
				.value(response -> {
					assert response != null;
					assert response.personalData() != null;
					assert response.personalData().email().equals("updated@example.com");
					assert response.personalData().firstName().equals("Updated");
					assert response.personalData().lastName().equals("User");
				});

		verify(customerService, times(1)).updateCustomer(eq(customerId), any(Customer.class));
	}

	@Test
	void deleteCustomer_ShouldReturnNoContent() {
		when(customerService.deleteCustomer(customerId)).thenReturn(Mono.empty());

		webTestClient.delete()
				.uri("/api/v1/customers/{id}", customerId)
				.exchange()
				.expectStatus().isNoContent();

		verify(customerService, times(1)).deleteCustomer(customerId);
	}
}