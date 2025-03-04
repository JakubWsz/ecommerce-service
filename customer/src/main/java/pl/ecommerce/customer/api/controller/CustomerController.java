package pl.ecommerce.customer.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.ecommerce.customer.api.CustomerMapper;
import pl.ecommerce.customer.api.dto.*;
import pl.ecommerce.customer.domain.model.Customer;
import pl.ecommerce.customer.domain.service.CustomerService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static pl.ecommerce.customer.api.CustomerMapper.toCustomer;

@Tag(name = "Customers", description = "Endpoints for customer management")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

	private final CustomerService customerService;

	@Operation(summary = "Register a new customer", description = "Creates a new customer based on the provided request data")
	@PostMapping
	public Mono<CustomerResponse> registerCustomer(@RequestBody CustomerRequest request,
												   @RequestParam(required = false) String ipAddress) {
		Customer customer = toCustomer(request);
		return customerService.registerCustomer(customer, ipAddress)
				.map(CustomerMapper::toResponse);
	}

	@Operation(summary = "Get customer by ID", description = "Returns a customer by its ID")
	@GetMapping("/{id}")
	public Mono<CustomerResponse> getCustomerById(@PathVariable UUID id) {
		return customerService.getCustomerById(id)
				.map(CustomerMapper::toResponse);
	}

	@Operation(summary = "Get customers", description = "Returns all customers")
	@GetMapping
	public Flux<CustomerResponse> getAllCustomers() {
		return customerService.getAllCustomers()
				.map(CustomerMapper::toResponse);
	}

	@Operation(summary = "Update customer", description = "Updates the details of an existing customer")
	@PutMapping("/{id}")
	public Mono<CustomerResponse> updateCustomer(@PathVariable UUID id, @RequestBody CustomerRequest request) {
		Customer customerUpdate = toCustomer(request);
		return customerService.updateCustomer(id, customerUpdate)
				.map(CustomerMapper::toResponse);
	}

	@Operation(summary = "Delete customer", description = "Deactivates a customer by ID")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> deleteCustomer(@PathVariable UUID id) {
		return customerService.deleteCustomer(id);
	}
}
