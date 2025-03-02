package pl.ecommerce.customer.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.ecommerce.customer.api.dto.*;
import pl.ecommerce.customer.domain.model.Customer;
import pl.ecommerce.customer.domain.model.PersonalData;
import pl.ecommerce.customer.domain.service.CustomerService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = CustomerController.class)
@Import(CustomerServiceTestConfiguration.class)
@ActiveProfiles("test")
public class CustomerControllerTest {

	@Autowired
	private CustomerService customerService;

	@Autowired
	private WebTestClient webTestClient;

	@BeforeEach
	public void setup() {
	}

	@Test
	public void testGetCustomerById() {
		PersonalData pd = new PersonalData("api@example.com", "Api", "User", "111222333");

		Customer customer = new Customer(
				true, "127.0.0.1", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
				true, LocalDateTime.now(), false, false, 730,
				pd, List.of(), null, null
		);
		when(customerService.getCustomerById(customer.getId())).thenReturn(Mono.just(customer));

		webTestClient.get().uri("/api/v1/customers/" + customer.getId())
				.exchange()
				.expectStatus().isOk()
				.expectBody(CustomerResponse.class)
				.value(response -> {
					assert response.personalData().email().equals("api@example.com");
				});
	}

	@Test
	public void testUpdateCustomer() {
		PersonalData pd = new PersonalData("updated@example.com", "Updated", "User", "000999888");

		Customer customer = new Customer(
				true, "127.0.0.1", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
				true, LocalDateTime.now(), false, false, 730,
				pd, List.of(), null, null
		);
		when(customerService.updateCustomer(any(UUID.class), any(Customer.class))).thenReturn(Mono.just(customer));

		UpdateCustomerRequest request = new UpdateCustomerRequest(
				new PersonalDataDto("updated@example.com", "Updated", "User", "000999888"),
				List.of()
		);

		webTestClient.put().uri("/api/v1/customers/" + customer.getId())
				.bodyValue(request)
				.exchange()
				.expectStatus().isOk()
				.expectBody(CustomerResponse.class)
				.value(response -> {
					assert response.personalData().email().equals("updated@example.com");
				});
	}

	@Test
	public void testDeleteCustomer() {
		UUID id = UUID.randomUUID();
		when(customerService.deleteCustomer(id)).thenReturn(Mono.empty());

		webTestClient.delete().uri("/api/v1/customers/" + id)
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	public void testGetAllCustomers() {
		PersonalData pd = new PersonalData( "list@example.com", "List", "User", "123123123");

		Customer customer = new Customer(
				true, "127.0.0.1",
				LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
				true, LocalDateTime.now(), false, false,
				730,
				pd, List.of(), null, null
		);

		when(customerService.getAllCustomers()).thenReturn(Flux.just(customer));

		webTestClient.get().uri("/api/v1/customers")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(CustomerResponse.class)
				.hasSize(1)
				.value(responseList -> {
					assert responseList.getFirst().personalData().email().equals("list@example.com");
				});
	}
}